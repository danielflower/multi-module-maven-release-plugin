package com.github.danielflower.mavenplugins.release.version;

import static java.lang.Long.valueOf;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

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

	void setRepository(final SCMRepository repository) {
		this.repository = repository;
	}

	void setFinder(final BuildNumberFinder finder) {
		this.finder = finder;
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
			final Long buildNumber, final String remoteUrl) throws VersionException {
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

		return new DefaultVersion(project.getVersion(), businessVersion, actualBuildNumber);
	}
}
