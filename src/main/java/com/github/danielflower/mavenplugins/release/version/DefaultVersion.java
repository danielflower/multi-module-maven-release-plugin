package com.github.danielflower.mavenplugins.release.version;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.scm.AnnotatedTag;
import com.github.danielflower.mavenplugins.release.scm.DiffDetector;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

final class DefaultVersion implements Version {
	private final SCMRepository gitRepo;
	private final MavenProject project;
	private final String versionWithoutBuildNumber;
	private final List<AnnotatedTag> previousTagsForThisModule;
	private final Collection<Long> remoteBuildNumbers;
	private Long buildNumber;

	DefaultVersion(final SCMRepository gitRepo, final MavenProject project, final String versionWithoutBuildNumber,
			final Long buildNumber) throws MojoExecutionException, ValidationException, GitAPIException {
		this.gitRepo = gitRepo;
		this.project = project;
		this.buildNumber = buildNumber;
		this.versionWithoutBuildNumber = versionWithoutBuildNumber;
		this.remoteBuildNumbers = gitRepo.getRemoteBuildNumbers(project.getArtifactId(), versionWithoutBuildNumber);
		previousTagsForThisModule = gitRepo.tagsForVersion(project.getArtifactId(), versionWithoutBuildNumber);

		init();
	}

	private void init() throws ValidationException, GitAPIException {
		final Collection<Long> previousBuildNumbers = new ArrayList<Long>();
		if (previousTagsForThisModule != null) {
			for (final AnnotatedTag previousTag : previousTagsForThisModule) {
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

		if (!Repository.isValidRefName("refs/tags/" + releaseVersion())) {
			final String summary = "Sorry, '" + releaseVersion() + "' is not a valid version.";
			throw new ValidationException(summary, asList(summary,
					"Version numbers are used in the Git tag, and so can only contain characters that are valid in git tags.",
					"Please see https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html for tag naming rules."));
		}
	}

	@Override
	public List<AnnotatedTag> getPreviousTagsForThisModule() throws MojoExecutionException {
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
	public AnnotatedTag hasChangedSinceLastRelease(final String relativePathToModule) throws MojoExecutionException {
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

	private AnnotatedTag tagWithHighestBuildNumber() {
		AnnotatedTag cur = null;
		for (final AnnotatedTag tag : previousTagsForThisModule) {
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
