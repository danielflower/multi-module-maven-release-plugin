package com.github.danielflower.mavenplugins.release.version;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.scm.DiffDetector;
import com.github.danielflower.mavenplugins.release.scm.ProposedTag;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

final class DefaultVersion implements Version {
	private final SCMRepository gitRepo;
	private final MavenProject project;
	private final String versionWithoutBuildNumber;
	private final List<ProposedTag> previousTagsForThisModule;
	private final Collection<Long> remoteBuildNumbers;
	private Long buildNumber;

	DefaultVersion(final SCMRepository gitRepo, final MavenProject project, final String versionWithoutBuildNumber,
			final Long buildNumber, final String remoteUrl)
					throws MojoExecutionException, ValidationException, GitAPIException {
		this.gitRepo = gitRepo;
		this.project = project;
		this.buildNumber = buildNumber;
		this.versionWithoutBuildNumber = versionWithoutBuildNumber;
		this.remoteBuildNumbers = gitRepo.getRemoteBuildNumbers(remoteUrl, project.getArtifactId(),
				versionWithoutBuildNumber);
		previousTagsForThisModule = gitRepo.tagsForVersion(project.getArtifactId(), versionWithoutBuildNumber);

		init();
	}

	private void init() throws ValidationException, GitAPIException {
		final Collection<Long> previousBuildNumbers = new ArrayList<Long>();
		if (previousTagsForThisModule != null) {
			for (final ProposedTag previousTag : previousTagsForThisModule) {
				previousBuildNumbers.add(previousTag.buildNumber());
			}
		}

		previousBuildNumbers.addAll(remoteBuildNumbers);

		if (buildNumber == null) {
			if (previousBuildNumbers.size() == 0) {
				buildNumber = 0L;
			} else {
				buildNumber = nextBuildNumber(previousBuildNumbers);
			}
		}

		gitRepo.checkValidRefName(releaseVersion());
	}

	@Override
	public List<ProposedTag> getPreviousTagsForThisModule() throws MojoExecutionException {
		return previousTagsForThisModule;
	}

	private static long nextBuildNumber(final Collection<Long> previousBuildNumbers) {
		long max = 0;
		for (final Long buildNumber : previousBuildNumbers) {
			max = Math.max(max, buildNumber);
		}
		return max + 1;
	}

	@Override
	public ProposedTag hasChangedSinceLastRelease(final String relativePathToModule) throws MojoExecutionException {
		try {
			if (previousTagsForThisModule.size() == 0) {
				return null;
			}
			final DiffDetector detector = gitRepo.newDiffDetector();
			final boolean hasChanged = detector.hasChangedSince(relativePathToModule, project.getModel().getModules(),
					previousTagsForThisModule);
			return hasChanged ? null : tagWithHighestBuildNumber();
		} catch (final Exception e) {
			throw new MojoExecutionException(
					format("Error while detecting whether or not %s has changed since the last release",
							project.getArtifactId()),
					e);
		}
	}

	private ProposedTag tagWithHighestBuildNumber() {
		ProposedTag cur = null;
		for (final ProposedTag tag : previousTagsForThisModule) {
			if (cur == null || tag.buildNumber() > cur.buildNumber()) {
				cur = tag;
			}
		}
		return cur;
	}

	/**
	 * For example, "1.0" if the development version is "1.0-SNAPSHOT"
	 */
	@Override
	public String businessVersion() {
		return versionWithoutBuildNumber;
	}

	@Override
	public long buildNumber() {
		return buildNumber;
	}

	/**
	 * The snapshot version, e.g. "1.0-SNAPSHOT"
	 */
	@Override
	public String developmentVersion() {
		return project.getVersion();
	}

	/**
	 * The business version with the build number appended, e.g. "1.0.1"
	 */
	@Override
	public String releaseVersion() {
		return businessVersion() + "." + buildNumber;
	}
}
