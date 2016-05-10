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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
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
@Named
@Singleton
public final class GitRepository implements SCMRepository {
	private final Log log;
	private final GitFactory gitFactory;
	private Git git;
	private ValidationException gitInstantiationException;
	private boolean hasReverted; // A premature optimisation? In
									// the normal case, file
									// reverting occurs twice, which
									// this bool prevents
	private Collection<Ref> remoteTags;

	@Inject
	public GitRepository(final Log log, final GitFactory gitFactory) {
		this.log = log;
		this.gitFactory = gitFactory;
	}

	private Git getGit() throws ValidationException {
		if (git == null && gitInstantiationException == null) {
			try {
				git = gitFactory.newGit();
			} catch (final ValidationException e) {
				gitInstantiationException = e;
			}
		}

		if (git == null) {
			throw gitInstantiationException;
		}

		return git;
	}

	@Override
	public Collection<Long> getRemoteBuildNumbers(final String remoteUrl, final String artifactId,
			final String versionWithoutBuildNumber) throws GitAPIException, ValidationException {
		final Collection<Ref> remoteTagRefs = allRemoteTags(remoteUrl);
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

	public Collection<Ref> allRemoteTags(final String remoteUrl) throws GitAPIException, ValidationException {
		if (remoteTags == null) {
			final LsRemoteCommand lsRemoteCommand = getGit().lsRemote().setTags(true).setHeads(false);
			if (remoteUrl != null) {
				lsRemoteCommand.setRemote(remoteUrl);
			}
			remoteTags = lsRemoteCommand.call();
		}
		return remoteTags;
	}

	@Override
	public boolean hasLocalTag(final String tagName) throws GitAPIException, ValidationException {
		return GitHelper.hasLocalTag(getGit(), tagName);
	}

	private Status currentStatus() throws ValidationException {
		Status status;
		try {
			status = getGit().status().call();
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

	private File workingDir() throws IOException, NoWorkTreeException, ValidationException {
		return getGit().getRepository().getWorkTree().getCanonicalFile();
	}

	@Override
	public boolean revertChanges(final List<File> changedFiles)
			throws IOException, NoWorkTreeException, ValidationException {
		if (hasReverted) {
			return true;
		}
		log.info("Going to revert changes because there was an error.");
		boolean hasErrors = false;
		final File workTree = workingDir();
		for (final File changedFile : changedFiles) {
			try {
				final String pathRelativeToWorkingTree = Repository.stripWorkDir(workTree, changedFile);
				getGit().checkout().addPath(pathRelativeToWorkingTree).call();
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
			throws MojoExecutionException, ValidationException {
		final List<ProposedTag> results = new ArrayList<>();
		List<Ref> tags;
		try {
			tags = getGit().tagList().call();
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
	public DiffDetector newDiffDetector() throws ValidationException {
		return new TreeWalkingDiffDetector(getGit().getRepository());
	}

	@Override
	public ProposedTag fromRef(final Ref gitTag) throws IOException, ValidationException {
		Guard.notNull("gitTag", gitTag);

		final RevWalk walk = new RevWalk(getGit().getRepository());
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
		return new DefaultProposedTag(getGit(), gitTag, stripRefPrefix(gitTag.getName()), message);
	}

	static String stripRefPrefix(final String refName) {
		return refName.substring("refs/tags/".length());
	}

	@Override
	public ProposedTagsBuilder newProposedTagsBuilder(final String remoteUrl) throws ValidationException {
		return new DefaultProposedTagsBuilder(log, getGit(), this, remoteUrl);
	}

}
