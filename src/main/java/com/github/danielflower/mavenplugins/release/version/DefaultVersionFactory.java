package com.github.danielflower.mavenplugins.release.version;

import static java.lang.Long.valueOf;
import static java.lang.String.format;

import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.github.danielflower.mavenplugins.release.scm.ProposedTag;
import com.github.danielflower.mavenplugins.release.scm.SCMException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

/**
 * Default implementation of the {@link VersionFactory} interface.
 *
 */
@Component(role = VersionFactory.class)
final class DefaultVersionFactory implements VersionFactory {
	static final String SNAPSHOT_EXTENSION = "-SNAPSHOT";

	@Requirement(role = SCMRepository.class)
	private SCMRepository repository;

	@Requirement(role = BuildNumberFinder.class)
	private BuildNumberFinder finder;

	@Requirement(role = Log.class)
	private Log log;

	void setRepository(final SCMRepository repository) {
		this.repository = repository;
	}

	void setFinder(final BuildNumberFinder finder) {
		this.finder = finder;
	}

	void setLog(final Log log) {
		this.log = log;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.danielflower.mavenplugins.release.version.VersionFactory#
	 * newVersioning(org.apache.maven.project.MavenProject, java.lang.Long,
	 * java.lang.String)
	 */
	@Override
	public Version newVersion(final MavenProject project, final boolean useLastDigitAsBuildNumber,
			final Long buildNumber, final String relativePathToModuleOrNull, final String changedDependencyOrNull,
			final String remoteUrl) throws VersionException {
		if (useLastDigitAsBuildNumber && buildNumber != null) {
			throw new VersionException("You cannot use 'useLastDigitAsBuildNumber' in conjunction with 'buildNumber'!");
		}

		String businessVersion = project.getVersion().replace(SNAPSHOT_EXTENSION, "");
		final long actualBuildNumber;
		if (buildNumber == null) {
			if (useLastDigitAsBuildNumber) {
				final int idx = businessVersion.lastIndexOf('.');
				actualBuildNumber = valueOf(businessVersion.substring(idx + 1));
				businessVersion = businessVersion.substring(0, idx);
			} else {
				actualBuildNumber = finder.findBuildNumber(project, remoteUrl, businessVersion);
			}
		} else {
			actualBuildNumber = buildNumber;
		}

		final String releaseVersion = businessVersion + "." + actualBuildNumber;
		final String equivalentVersion = logReleaseInfo(project, changedDependencyOrNull, businessVersion,
				releaseVersion, relativePathToModuleOrNull);

		return new DefaultVersion(equivalentVersion, releaseVersion, project.getVersion(), businessVersion,
				actualBuildNumber, useLastDigitAsBuildNumber);
	}

	private String logReleaseInfo(final MavenProject project, final String changedDependencyOrNull,
			final String businessVersion, final String releaseVersion, final String relativePathToModuleOrNull)
					throws VersionException {
		String equivalentVersion = null;

		if (relativePathToModuleOrNull == null) {
			log.info(format("Releasing %s %s as we was asked to forced release.", project.getArtifactId(),
					releaseVersion));
			return null;
		}

		if (changedDependencyOrNull != null) {
			log.info(format("Releasing %s %s as %s has changed.", project.getArtifactId(), releaseVersion,
					changedDependencyOrNull));
			return null;
		}

		final ProposedTag previousTagThatIsTheSameAsHEADForThisModule = hasChangedSinceLastRelease(project,
				businessVersion, relativePathToModuleOrNull);
		if (previousTagThatIsTheSameAsHEADForThisModule == null) {
			log.info(format("Will use version %s for %s as it has changed since the last release.", releaseVersion,
					project.getArtifactId()));
			return null;
		}

		equivalentVersion = previousTagThatIsTheSameAsHEADForThisModule.version() + "."
				+ previousTagThatIsTheSameAsHEADForThisModule.buildNumber();
		log.info(format("Will use version %s for %s as it has not been changed since that release.", equivalentVersion,
				project.getArtifactId()));
		return equivalentVersion;
	}

	private ProposedTag hasChangedSinceLastRelease(final MavenProject project, final String businessVersion,
			final String relativePathToModule) throws VersionException {
		try {
			final List<ProposedTag> previousTagsForThisModule = repository.tagsForVersion(project.getArtifactId(),
					businessVersion);
			if (previousTagsForThisModule.size() == 0) {
				return null;
			}
			final boolean hasChanged = repository.hasChangedSince(relativePathToModule, project.getModules(),
					previousTagsForThisModule);
			return hasChanged ? null : tagWithHighestBuildNumber(previousTagsForThisModule);
		} catch (final SCMException e) {
			throw new VersionException(e, "Error while detecting whether or not %s has changed since the last release",
					project.getArtifactId());
		}
	}

	private ProposedTag tagWithHighestBuildNumber(final List<ProposedTag> previousTagsForThisModule) {
		ProposedTag cur = null;
		for (final ProposedTag tag : previousTagsForThisModule) {
			if (cur == null || tag.buildNumber() > cur.buildNumber()) {
				cur = tag;
			}
		}
		return cur;
	}
}
