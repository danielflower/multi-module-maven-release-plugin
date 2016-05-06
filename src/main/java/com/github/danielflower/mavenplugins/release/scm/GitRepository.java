package com.github.danielflower.mavenplugins.release.scm;

import static com.github.danielflower.mavenplugins.release.scm.DefaultProposedTag.BUILD_NUMBER;
import static com.github.danielflower.mavenplugins.release.scm.DefaultProposedTag.VERSION;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.github.danielflower.mavenplugins.release.Guard;
import com.github.danielflower.mavenplugins.release.ValidationException;

// TODO: Make this class package private when SingleModuleTest is working with a Guice injector
public final class GitRepository implements SCMRepository {
	private final Log log;
	private final Git git;
	private final String remoteUrl;
	private boolean hasReverted; // A premature optimisation? In
									// the normal case, file
									// reverting occurs twice, which
									// this bool prevents
	private Collection<Ref> remoteTags;

	public GitRepository(final Log log, final Git git, final String remoteUrl) {
		this.log = log;
		this.git = git;
		this.remoteUrl = remoteUrl;
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
	public boolean revertChanges(final List<File> changedFiles) throws IOException {
		if (hasReverted) {
			return true;
		}
		log.info("Going to revert changes because there was an error.");
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
	public List<ProposedTag> tagsForVersion(final String module, final String versionWithoutBuildNumber)
			throws MojoExecutionException {
		final List<ProposedTag> results = new ArrayList<>();
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
					results.add(fromRef(tag));
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
		final String tagName = stripRefPrefix(refName);
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

	@Override
	public ProposedTag fromRef(final Ref gitTag) throws IOException {
		Guard.notNull("gitTag", gitTag);

		final RevWalk walk = new RevWalk(git.getRepository());
		JSONObject message;
		try {
			final ObjectId tagId = gitTag.getObjectId();
			final RevTag tag = walk.parseTag(tagId);
			message = (JSONObject) JSONValue.parse(tag.getFullMessage());
		} finally {
			walk.dispose();
		}
		if (message == null) {
			message = new JSONObject();
			message.put(VERSION, "0");
			message.put(BUILD_NUMBER, "0");
		}
		return new DefaultProposedTag(git, gitTag, stripRefPrefix(gitTag.getName()), message);
	}

	static String stripRefPrefix(final String refName) {
		return refName.substring("refs/tags/".length());
	}

	@Override
	public ProposedTags newProposedTags() {
		return new DefaultProposedTags(log, git, this, remoteUrl);
	}
}
