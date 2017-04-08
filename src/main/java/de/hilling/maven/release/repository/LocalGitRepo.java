package de.hilling.maven.release.repository;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;

import de.hilling.maven.release.AnnotatedTag;
import de.hilling.maven.release.FileUtils;
import de.hilling.maven.release.GitHelper;
import de.hilling.maven.release.ValidationException;

public class LocalGitRepo {

    public final  Git    git;
    private final String remoteUrl;
    private final Log    log;
    private boolean hasReverted = false; // A premature optimisation? In the normal case, file reverting occurs twice, which this bool prevents
    private Collection<Ref> remoteTags;

    LocalGitRepo(Git git, String remoteUrl, Log log) {
        this.git = git;
        this.remoteUrl = remoteUrl;
        this.log = log;
    }

    /**
     * Uses the current working dir to open the Git repository.
     *
     * @param remoteUrl The value in pom.scm.connection or null if none specified, in which case the default remote is used.
     * @param log       maven logger
     *
     * @throws ValidationException if anything goes wrong
     */
    public static LocalGitRepo fromCurrentDir(String remoteUrl, Log log) throws ValidationException {
        Git git;
        File gitDir = new File(".");
        try {
            git = Git.open(gitDir);
        } catch (RepositoryNotFoundException rnfe) {
            String fullPathOfCurrentDir = FileUtils.pathOf(gitDir);
            File gitRoot = getGitRootIfItExistsInOneOfTheParentDirectories(new File(fullPathOfCurrentDir));
            String summary;
            List<String> messages = new ArrayList<>();
            if (gitRoot == null) {
                summary = "Releases can only be performed from Git repositories.";
                messages.add(summary);
                messages.add(fullPathOfCurrentDir + " is not a Git repository.");
            } else {
                summary = "The release plugin can only be run from the root folder of your Git repository";
                messages.add(summary);
                messages.add(fullPathOfCurrentDir + " is not the root of a Gir repository");
                messages.add("Try running the release plugin from " + FileUtils.pathOf(gitRoot));
            }
            throw new ValidationException(summary, messages);
        } catch (Exception e) {
            throw new ValidationException("Could not open git repository. Is " + FileUtils.pathOf(
                gitDir) + " a git repository?", Arrays.asList("Exception returned when accessing the git repo:",
                                                              e.toString()));
        }
        return new LocalGitRepo(git, remoteUrl, log);
    }

    private static File getGitRootIfItExistsInOneOfTheParentDirectories(File candidateDir) {
        while (candidateDir != null && /* HACK ATTACK! Maybe.... */ !candidateDir.getName().equals("target")) {
            if (new File(candidateDir, ".git").isDirectory()) {
                return candidateDir;
            }
            candidateDir = candidateDir.getParentFile();
        }
        return null;
    }

    public void errorIfNotClean() throws ValidationException {
        Status status = currentStatus();
        boolean isClean = status.isClean();
        if (!isClean) {
            String summary = "Cannot release with uncommitted changes. Please check the following files:";
            List<String> message = new ArrayList<>();
            message.add(summary);
            Set<String> uncommittedChanges = status.getUncommittedChanges();
            if (uncommittedChanges.size() > 0) {
                message.add("Uncommitted:");
                for (String path : uncommittedChanges) {
                    message.add(" * " + path);
                }
            }
            Set<String> untracked = status.getUntracked();
            if (untracked.size() > 0) {
                message.add("Untracked:");
                for (String path : untracked) {
                    message.add(" * " + path);
                }
            }
            message.add("Please commit or revert these changes before releasing.");
            throw new ValidationException(summary, message);
        }
    }

    private Status currentStatus() throws ValidationException {
        Status status;
        try {
            status = git.status().call();
        } catch (GitAPIException e) {
            throw new ValidationException("Error while checking if the Git repo is clean", e);
        }
        return status;
    }

    public boolean revertChanges(Log log, List<File> changedFiles) throws MojoExecutionException {
        if (hasReverted) {
            return true;
        }
        boolean hasErrors = false;
        File workTree = workingDir();
        for (File changedFile : changedFiles) {
            try {
                String pathRelativeToWorkingTree = Repository.stripWorkDir(workTree, changedFile);
                git.checkout().addPath(pathRelativeToWorkingTree).call();
            } catch (Exception e) {
                hasErrors = true;
                log.error(
                    "Unable to revert changes to " + changedFile + " - you may need to manually revert this file. Error was: " + e.getMessage());
            }
        }
        hasReverted = true;
        return !hasErrors;
    }

    private File workingDir() throws MojoExecutionException {
        try {
            return git.getRepository().getWorkTree().getCanonicalFile();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not locate the working directory of the Git repo", e);
        }
    }

    boolean hasLocalTag(String tagName) throws GitAPIException {
        return GitHelper.hasLocalTag(git, tagName);
    }

    public void tagRepo(AnnotatedTag tag) throws GitAPIException {
        tag.saveAtHEAD(git);
    }

    public void pushAll() throws GitAPIException {
        PushCommand pushAll = git.push().setPushAll().setPushTags();
        if (remoteUrl != null) {
            pushAll.setRemote(remoteUrl);
        }
        pushAll.call().iterator().forEachRemaining(this::logResult);
    }

    private void logResult(PushResult m) {
        log.debug("push: " + m.getRemoteUpdates().stream().map(RemoteRefUpdate::toString).collect(joining(",")));
    }

    public Optional<Ref> getRemoteTag(String tagName) throws GitAPIException {
        List<String> results = new ArrayList<>();
        Collection<Ref> remoteTags = allRemoteTags();
        for (Ref remoteTag : remoteTags) {
            if (remoteTag.getName().equals("refs/tags/" + tagName)) {
                return Optional.of(remoteTag);
            }
        }
        return Optional.empty();
    }

    private Collection<Ref> allRemoteTags() throws GitAPIException {
        if (remoteTags == null) {
            LsRemoteCommand lsRemoteCommand = git.lsRemote().setTags(true).setHeads(false);
            if (remoteUrl != null) {
                lsRemoteCommand.setRemote(remoteUrl);
            }
            remoteTags = lsRemoteCommand.call();
        }
        return remoteTags;
    }
}
