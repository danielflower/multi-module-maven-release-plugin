package com.github.danielflower.mavenplugins.release.scm;

import static com.github.danielflower.mavenplugins.release.scm.DefaultProposedTag.BUILD_NUMBER;
import static com.github.danielflower.mavenplugins.release.scm.DefaultProposedTag.VERSION;
import static java.lang.String.format;
import static org.eclipse.jgit.lib.Repository.isValidRefName;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
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

// TODO: Make this class package private when SingleModuleTest is working with a Guice injector
@Component(role = SCMRepository.class)
public final class GitRepository implements SCMRepository {
	private static final String REFS_TAGS = "refs/tags/";

	static final String INVALID_REF_NAME_MESSAGE = "Sorry, '%s' is not a valid version.";

	@Requirement(role = Log.class)
	private Log log;

	@Requirement(role = GitFactory.class)
	private GitFactory gitFactory;

	private Git git;
	private SCMException gitInstantiationException;
	private boolean hasReverted; // A premature optimisation? In
									// the normal case, file
									// reverting occurs twice, which
									// this bool prevents
	private Collection<Ref> remoteTags;

	public void setLog(final Log log) {
		this.log = log;
	}

	public void setGitFactory(final GitFactory gitFactory) {
		this.gitFactory = gitFactory;
	}

	private Git getGit() throws SCMException {
		if (git == null && gitInstantiationException == null) {
			try {
				git = gitFactory.newGit();
			} catch (final SCMException e) {
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
			final String versionWithoutBuildNumber) throws SCMException {
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

	public Collection<Ref> allRemoteTags(final String remoteUrl) throws SCMException {
		if (remoteTags == null) {
			final LsRemoteCommand lsRemoteCommand = getGit().lsRemote().setTags(true).setHeads(false);
			if (remoteUrl != null) {
				lsRemoteCommand.setRemote(remoteUrl);
			}
			try {
				remoteTags = lsRemoteCommand.call();
			} catch (final GitAPIException e) {
				throw new SCMException(e, "Remote tags could not be listed!");
			}
		}
		return remoteTags;
	}

	@Override
	public boolean hasLocalTag(final String tagName) throws SCMException {
		try {
			return GitHelper.hasLocalTag(getGit(), tagName);
		} catch (final GitAPIException e) {
			throw new SCMException(e, "Local tag could not be determined!");
		}
	}

	private Status currentStatus() throws SCMException {
		Status status;
		try {
			status = getGit().status().call();
		} catch (final GitAPIException e) {
			throw new SCMException(e, "Error while checking if the Git repo is clean");
		}
		return status;
	}

	@Override
	public void errorIfNotClean() throws SCMException {
		final Status status = currentStatus();
		final boolean isClean = status.isClean();
		if (!isClean) {
			final SCMException exception = new SCMException(
					"Cannot release with uncommitted changes. Please check the following files:");
			final Set<String> uncommittedChanges = status.getUncommittedChanges();
			if (uncommittedChanges.size() > 0) {
				exception.add("Uncommitted:");
				for (final String path : uncommittedChanges) {
					exception.add(" * %s", path);
				}
			}
			final Set<String> untracked = status.getUntracked();
			if (untracked.size() > 0) {
				exception.add("Untracked:");
				for (final String path : untracked) {
					exception.add(" * %s", path);
				}
			}
			throw exception.add("Please commit or revert these changes before releasing.");
		}
	}

	private File workingDir() throws SCMException {
		try {
			return getGit().getRepository().getWorkTree().getCanonicalFile();
		} catch (NoWorkTreeException | IOException e) {
			throw new SCMException(e, "Working directory could not be determined!");
		}
	}

	@Override
	public boolean revertChanges(final List<File> changedFiles) throws SCMException {
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
			throws SCMException {
		final List<ProposedTag> results = new ArrayList<>();
		List<Ref> tags;
		try {
			tags = getGit().tagList().call();
		} catch (final GitAPIException e) {
			throw new SCMException(e, "Error while getting a list of tags in the local repo");
		}
		Collections.reverse(tags);
		final String tagWithoutBuildNumber = module + "-" + versionWithoutBuildNumber;
		for (final Ref tag : tags) {
			if (isPotentiallySameVersionIgnoringBuildNumber(tagWithoutBuildNumber, tag.getName())) {
				results.add(fromRef(tag));
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
	public DiffDetector newDiffDetector() throws SCMException {
		return new TreeWalkingDiffDetector(getGit().getRepository());
	}

	@Override
	public ProposedTag fromRef(final Ref gitTag) throws SCMException {
		Guard.notNull("gitTag", gitTag);

		final RevWalk walk = new RevWalk(getGit().getRepository());
		final ObjectId tagId = gitTag.getObjectId();
		JSONObject message;
		try {
			final RevTag tag = walk.parseTag(tagId);
			message = (JSONObject) JSONValue.parse(tag.getFullMessage());
		} catch (final IOException e) {
			throw new SCMException(e, "Error while looking up tag because RevTag could not be parsed! Object-id was %s",
					tagId);
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
		return refName.substring(REFS_TAGS.length());
	}

	@Override
	public ProposedTagsBuilder newProposedTagsBuilder(final String remoteUrl) throws SCMException {
		return new DefaultProposedTagsBuilder(log, getGit(), this, remoteUrl);
	}

	@Override
	public void checkValidRefName(final String releaseVersion) throws SCMException {
		if (!isValidRefName(format("%s%s", REFS_TAGS, releaseVersion))) {
			throw new SCMException(INVALID_REF_NAME_MESSAGE, releaseVersion)
					.add("Version numbers are used in the Git tag, and so can only contain characters that are valid in git tags.")
					.add("Please see https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html for tag naming rules.");
		}
	}
}
