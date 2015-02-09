package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.github.danielflower.mavenplugins.release.FileUtils.pathOf;

public class LocalGitRepo {

    private final Git git;

    LocalGitRepo(Git git) {
        this.git = git;
    }

    public void errorIfNotClean() throws ValidationException {
        Status status = currentStatus();
        boolean isClean = status.isClean();
        if (!isClean) {
            String summary = "Cannot release with uncommitted changes. Please check the following files:";
            List<String> message = new ArrayList<String>();
            message.add(summary);
            for (String path : status.getUncommittedChanges()) {
                message.add(" * " + path);
            }
            for (String path : status.getUntracked()) {
                message.add(" * " + path);
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
        boolean hasErrors = false;
        File workTree = git.getRepository().getWorkTree();
        for (File changedFile : changedFiles) {
            try {
                String pathRelativeToWorkingTree = Repository.stripWorkDir(workTree, changedFile);
                git.checkout().addPath(pathRelativeToWorkingTree).call();
            } catch (Exception e) {
                hasErrors = true;
                log.error("Unable to revert changes to " + changedFile + " - you may need to manually revert this file. Error was: " + e.getMessage());
            }
        }
        return !hasErrors;
    }

    public boolean hasLocalTag(String tagName) throws GitAPIException {
        return GitHelper.hasLocalTag(git, tagName);
    }

    public void tagRepoAndPush(String tag) throws GitAPIException {
        Ref tagRef = git.tag().setAnnotated(true).setName(tag).setMessage("Release " + tag).call();
        git.push().add(tagRef).call();
    }

    public static LocalGitRepo fromCurrentDir() throws ValidationException {
        Git git;
        File gitDir = new File(".");
        try {
            git = Git.open(gitDir);
        } catch (RepositoryNotFoundException rnfe) {
            String summary = "Releases can only be performed from Git repositories.";
            List<String> messages = new ArrayList<String>();
            messages.add(summary);
            messages.add(pathOf(gitDir) + " is not a Git repository.");
            throw new ValidationException(summary, messages);
        } catch (Exception e) {
            throw new ValidationException("Could not open git repository. Is " + pathOf(gitDir) + " a git repository?", Arrays.asList("Exception returned when accessing the git repo:", e.toString()));
        }
        return new LocalGitRepo(git);
    }

    public List<String> remoteTagsFrom(List<String> tagNames) throws GitAPIException {
        List<String> results = new ArrayList<String>();
        Collection<Ref> remoteTags = git.lsRemote().setTags(true).setHeads(false).call();
        for (Ref remoteTag : remoteTags) {
            for (String proposedTagName : tagNames) {
                if (remoteTag.getName().equals("refs/tags/" + proposedTagName)) {
                    results.add(proposedTagName);
                }
            }
        }
        return results;
    }
}
