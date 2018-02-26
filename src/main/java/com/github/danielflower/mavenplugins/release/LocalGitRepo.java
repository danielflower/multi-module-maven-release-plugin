package com.github.danielflower.mavenplugins.release;

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

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.github.danielflower.mavenplugins.release.FileUtils.pathOf;

public class LocalGitRepo {

    public final Git git;
    private boolean hasReverted = false; // A premature optimisation? In the normal case, file reverting occurs twice, which this bool prevents
    private Collection<Ref> tags;

    private final TagFetcher tagFetcher;
    private final TagPusher tagPusher;

    public static class Builder {

        private Set<GitOperations> operationsAllowed = EnumSet.allOf(GitOperations.class);
        private String remoteGitUrl;

        /**
         * Flag for which remote Git operations are permitted. Local values will
         * be substituted if remote operations are forbidden; this means the
         * local copy of the repository must be up to date!
         */
        public Builder remoteGitOperationsAllowed(Set<GitOperations> operationsAllowed) {
            this.operationsAllowed = EnumSet.copyOf(operationsAllowed);
            return this;
        }

        /**
         * Overrides the URL of remote Git repository.
         */
        public Builder remoteGitUrl(String remoteUrl) {
            this.remoteGitUrl = remoteUrl;
            return this;
        }

        /**
         * Uses the current working dir to open the Git repository.
         * @throws ValidationException if anything goes wrong
         */
        public LocalGitRepo buildFromCurrentDir() throws ValidationException {
            Git git;
            File gitDir = new File(".");
            try {
                git = Git.open(gitDir);
            } catch (RepositoryNotFoundException rnfe) {
                String fullPathOfCurrentDir = pathOf(gitDir);
                File gitRoot = getGitRootIfItExistsInOneOfTheParentDirectories(new File(fullPathOfCurrentDir));
                String summary;
                List<String> messages = new ArrayList<String>();
                if (gitRoot == null) {
                    summary = "Releases can only be performed from Git repositories.";
                    messages.add(summary);
                    messages.add(fullPathOfCurrentDir + " is not a Git repository.");
                } else {
                    summary = "The release plugin can only be run from the root folder of your Git repository";
                    messages.add(summary);
                    messages.add(fullPathOfCurrentDir + " is not the root of a Git repository");
                    messages.add("Try running the release plugin from " + pathOf(gitRoot));
                }
                throw new ValidationException(summary, messages);
            } catch (Exception e) {
                throw new ValidationException("Could not open git repository. Is " + pathOf(gitDir) + " a git repository?", Arrays.asList("Exception returned when accessing the git repo:", e.toString()));
            }

            TagFetcher tagFetcher;
            TagPusher tagPusher;

            if (operationsAllowed.contains(GitOperations.PULL_TAGS)) {
                tagFetcher = new RemoteTagFetcher(git, remoteGitUrl);
            } else {
                tagFetcher = new LocalTagFetcher(git);
            }

            if (operationsAllowed.contains(GitOperations.PUSH_TAGS)) {
                tagPusher = new RemoteTagPusher(git, remoteGitUrl);
            } else {
                tagPusher = new LocalTagPusher(git);
            }

            return new LocalGitRepo(git, tagFetcher, tagPusher);
        }

    }

    LocalGitRepo(Git git, TagFetcher tagFetcher, TagPusher tagPusher) {
        this.git = git;
        this.tagFetcher = tagFetcher;
        this.tagPusher = tagPusher;
    }

    public void errorIfNotClean() throws ValidationException {
        Status status = currentStatus();
        boolean isClean = status.isClean();
        if (!isClean) {
            String summary = "Cannot release with uncommitted changes. Please check the following files:";
            List<String> message = new ArrayList<String>();
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
                log.error("Unable to revert changes to " + changedFile + " - you may need to manually revert this file. Error was: " + e.getMessage());
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

    public boolean hasLocalTag(String tagName) throws GitAPIException {
        return GitHelper.hasLocalTag(git, tagName);
    }

    public void tagAndPushRepo(Collection<AnnotatedTag> tags) throws GitAPIException {
        tagPusher.pushTags(tags);
    }

    private static File getGitRootIfItExistsInOneOfTheParentDirectories(File candidateDir) {
        while (candidateDir != null && /* HACK ATTACK! Maybe.... */ !candidateDir.getName().equals("target") ) {
            if (new File(candidateDir, ".git").isDirectory()) {
                return candidateDir;
            }
            candidateDir = candidateDir.getParentFile();
        }
        return null;
    }

    public List<String> tagsFrom(List<AnnotatedTag> annotatedTags) throws GitAPIException {
        List<String> tagNames = new ArrayList<String>();
        for (AnnotatedTag annotatedTag : annotatedTags) {
            tagNames.add(annotatedTag.name());
        }
        return getTags(tagNames);
    }

    public List<String> getTags(List<String> tagNamesToSearchFor) throws GitAPIException {
        List<String> results = new ArrayList<String>();
        Collection<Ref> remoteTags = allTags();
        for (Ref remoteTag : remoteTags) {
            for (String proposedTag : tagNamesToSearchFor) {
                if (remoteTag.getName().equals("refs/tags/" + proposedTag)) {
                    results.add(proposedTag);
                }
            }
        }
        return results;
    }

    public Collection<Ref> allTags() throws GitAPIException {
        if (tags == null) {
            tags = this.tagFetcher.getTags();
        }

        return tags;
    }
}

interface TagFetcher {

    public Collection<Ref> getTags() throws GitAPIException;

}

class RemoteTagFetcher implements TagFetcher {

    private final Git git;
    private final String remoteUrl;

    public RemoteTagFetcher(Git git, String remoteUrl) {
        this.git = git;
        this.remoteUrl = remoteUrl;
    }

    @Override
    public Collection<Ref> getTags() throws GitAPIException {
        LsRemoteCommand lsRemoteCommand = git.lsRemote().setTags(true).setHeads(false);
        if (remoteUrl != null) {
            lsRemoteCommand.setRemote(remoteUrl);
        }

        return lsRemoteCommand.call();
   }

}

class LocalTagFetcher implements TagFetcher {

    private final Git git;

    public LocalTagFetcher(Git git) {
        this.git = git;
    }

    @Override
    public Collection<Ref> getTags() throws GitAPIException {
        return git.tagList().call();
    }

}

interface TagPusher {

    public void pushTags(Collection<AnnotatedTag> tags) throws GitAPIException;

}

class RemoteTagPusher implements TagPusher {

    private final Git git;
    private final String remoteUrl;

    public RemoteTagPusher(Git git, String remoteUrl) {
        this.git = git;
        this.remoteUrl = remoteUrl;
    }

    @Override
    public void pushTags(Collection<AnnotatedTag> tags) throws GitAPIException {
        PushCommand pushCommand = git.push();
        if (remoteUrl != null) {
            pushCommand.setRemote(remoteUrl);
        }

        for (AnnotatedTag tag : tags) {
            pushCommand.add(tag.saveAtHEAD(git));
        }

        pushCommand.call();
    }

}

class LocalTagPusher implements TagPusher {

    private final Git git;

    public LocalTagPusher(Git git) {
        this.git = git;
    }

    @Override
    public void pushTags(Collection<AnnotatedTag> tags) throws GitAPIException {
        for (AnnotatedTag tag : tags) {
            tag.saveAtHEAD(git);
        }
    }

}
