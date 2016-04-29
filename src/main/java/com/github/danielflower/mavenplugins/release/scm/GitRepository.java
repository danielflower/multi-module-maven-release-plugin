package com.github.danielflower.mavenplugins.release.scm;

import static com.github.danielflower.mavenplugins.release.FileUtils.pathOf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import com.github.danielflower.mavenplugins.release.ValidationException;

final class GitRepository implements SCMRepository {
	private Git git;
	private final String remoteUrl;
	private boolean hasReverted = false; // A premature optimisation? In
											// the normal case, file
											// reverting occurs twice, which
											// this bool prevents
	private Collection<Ref> remoteTags;

	GitRepository(final MavenProject project) {
		final File gitDir = new File(".");
		try {
			git = Git.open(gitDir);
		} catch (final RepositoryNotFoundException rnfe) {
			final String fullPathOfCurrentDir = pathOf(gitDir);
			final File gitRoot = getGitRootIfItExistsInOneOfTheParentDirectories(new File(fullPathOfCurrentDir));
			String summary;
			final List<String> messages = new ArrayList<String>();
			if (gitRoot == null) {
				summary = "Releases can only be performed from Git repositories.";
				messages.add(summary);
				messages.add(fullPathOfCurrentDir + " is not a Git repository.");
			} else {
				summary = "The release plugin can only be run from the root folder of your Git repository";
				messages.add(summary);
				messages.add(fullPathOfCurrentDir + " is not the root of a Gir repository");
				messages.add("Try running the release plugin from " + pathOf(gitRoot));
			}
			throw new ValidationException(summary, messages);
		} catch (final Exception e) {
			throw new ValidationException("Could not open git repository. Is " + pathOf(gitDir) + " a git repository?",
					Arrays.asList("Exception returned when accessing the git repo:", e.toString()));
		}
		remoteUrl = getRemoteUrlOrNullIfNoneSet(project.getScm());
	}

	GitRepository(final Git git, final String remoteUrl) {
		this.git = git;
		this.remoteUrl = remoteUrl;
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

	private String getRemoteUrlOrNullIfNoneSet(final Scm scm) throws ValidationException {
		if (scm == null) {
			return null;
		}
		String remote = scm.getDeveloperConnection();
		if (remote == null) {
			remote = scm.getConnection();
		}
		if (remote == null) {
			return null;
		}
		return GitHelper.scmUrlToRemote(remote);
	}

	@Override
	public Collection<Long> getRemoteBuildNumbers(final String artifactId, final String versionWithoutBuildNumber)
			throws GitAPIException {
		final Collection<Ref> remoteTagRefs = allRemoteTags();
		final Collection<Long> remoteBuildNumbers = new ArrayList<Long>();
		final String tagWithoutBuildNumber = artifactId + "-" + versionWithoutBuildNumber;
		for (final Ref remoteTagRef : remoteTagRefs) {
			final String remoteTagName = remoteTagRef.getName();
			final Long buildNumber = buildNumberOf(tagWithoutBuildNumber, remoteTagName);
			if (buildNumber != null) {
				remoteBuildNumbers.add(buildNumber);
			}
		}
		return remoteBuildNumbers;
	}

	@Override
	public List<String> remoteTagsFrom(final List<AnnotatedTag> annotatedTags) throws GitAPIException {
		final List<String> tagNames = new ArrayList<String>();
		for (final AnnotatedTag annotatedTag : annotatedTags) {
			tagNames.add(annotatedTag.name());
		}
		return getRemoteTags(tagNames);
	}

	public List<String> getRemoteTags(final List<String> tagNamesToSearchFor) throws GitAPIException {
		final List<String> results = new ArrayList<String>();
		final Collection<Ref> remoteTags = allRemoteTags();
		for (final Ref remoteTag : remoteTags) {
			for (final String proposedTag : tagNamesToSearchFor) {
				if (remoteTag.getName().equals("refs/tags/" + proposedTag)) {
					results.add(proposedTag);
				}
			}
		}
		return results;
	}

	public Collection<Ref> allRemoteTags() throws GitAPIException {
		if (remoteTags == null) {
			final LsRemoteCommand lsRemoteCommand = git.lsRemote().setTags(true).setHeads(false);
			if (remoteUrl != null) {
				lsRemoteCommand.setRemote(remoteUrl);
			}
			remoteTags = lsRemoteCommand.call();
		}
		return remoteTags;
	}

	@Override
	public void tagRepoAndPush(final AnnotatedTag tag) throws GitAPIException {
		final Ref tagRef = tag.saveAtHEAD(git);
		final PushCommand pushCommand = git.push().add(tagRef);
		if (remoteUrl != null) {
			pushCommand.setRemote(remoteUrl);
		}
		pushCommand.call();
	}

	@Override
	public boolean hasLocalTag(final String tagName) throws GitAPIException {
		return GitHelper.hasLocalTag(git, tagName);
	}

	private Status currentStatus() throws ValidationException {
		Status status;
		try {
			status = git.status().call();
		} catch (final GitAPIException e) {
			throw new ValidationException("Error while checking if the Git repo is clean", e);
		}
		return status;
	}

	@Override
	public void errorIfNotClean() throws ValidationException {
		final Status status = currentStatus();
		final boolean isClean = status.isClean();
		if (!isClean) {
			final String summary = "Cannot release with uncommitted changes. Please check the following files:";
			final List<String> message = new ArrayList<String>();
			message.add(summary);
			final Set<String> uncommittedChanges = status.getUncommittedChanges();
			if (uncommittedChanges.size() > 0) {
				message.add("Uncommitted:");
				for (final String path : uncommittedChanges) {
					message.add(" * " + path);
				}
			}
			final Set<String> untracked = status.getUntracked();
			if (untracked.size() > 0) {
				message.add("Untracked:");
				for (final String path : untracked) {
					message.add(" * " + path);
				}
			}
			message.add("Please commit or revert these changes before releasing.");
			throw new ValidationException(summary, message);
		}
	}

	private File workingDir() throws IOException {
		return git.getRepository().getWorkTree().getCanonicalFile();
	}

	@Override
	public boolean revertChanges(final Log log, final List<File> changedFiles) throws IOException {
		if (hasReverted) {
			return true;
		}
		boolean hasErrors = false;
		final File workTree = workingDir();
		for (final File changedFile : changedFiles) {
			try {
				final String pathRelativeToWorkingTree = Repository.stripWorkDir(workTree, changedFile);
				git.checkout().addPath(pathRelativeToWorkingTree).call();
			} catch (final Exception e) {
				hasErrors = true;
				log.error("Unable to revert changes to " + changedFile
						+ " - you may need to manually revert this file. Error was: " + e.getMessage());
			}
		}
		hasReverted = true;
		return !hasErrors;
	}

	@Override
	public List<AnnotatedTag> tagsForVersion(final String module, final String versionWithoutBuildNumber)
			throws MojoExecutionException {
		final ArrayList<AnnotatedTag> results = new ArrayList<AnnotatedTag>();
		List<Ref> tags;
		try {
			tags = git.tagList().call();
		} catch (final GitAPIException e) {
			throw new MojoExecutionException("Error while getting a list of tags in the local repo", e);
		}
		Collections.reverse(tags);
		final String tagWithoutBuildNumber = module + "-" + versionWithoutBuildNumber;
		for (final Ref tag : tags) {
			if (isPotentiallySameVersionIgnoringBuildNumber(tagWithoutBuildNumber, tag.getName())) {
				try {
					results.add(AnnotatedTag.fromRef(git.getRepository(), tag));
				} catch (final IOException e) {
					throw new MojoExecutionException("Error while looking up tag " + tag, e);
				}
			}
		}
		return results;
	}

	static boolean isPotentiallySameVersionIgnoringBuildNumber(final String versionWithoutBuildNumber,
			final String refName) {
		return buildNumberOf(versionWithoutBuildNumber, refName) != null;
	}

	public static Long buildNumberOf(final String versionWithoutBuildNumber, final String refName) {
		final String tagName = AnnotatedTag.stripRefPrefix(refName);
		final String prefix = versionWithoutBuildNumber + ".";
		if (tagName.startsWith(prefix)) {
			final String end = tagName.substring(prefix.length());
			try {
				return Long.parseLong(end);
			} catch (final NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	@Override
	public DiffDetector newDiffDetector() {
		return new TreeWalkingDiffDetector(git.getRepository());
	}
}
