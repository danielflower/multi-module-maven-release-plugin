package com.github.danielflower.mavenplugins.release.version;

import static java.lang.Long.valueOf;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Default implementation of the {@link VersionFactory} interface.
 *
 */
@Component(role = VersionFactory.class)
final class DefaultVersionFactory implements VersionFactory {
	static final String SNAPSHOT_EXTENSION = "-SNAPSHOT";

	@Requirement(role = BuildNumberFinder.class)
	private BuildNumberFinder finder;

	@Requirement(role = VersionBuilderFactory.class)
	private VersionBuilderFactory versionBuilderFactory;

	void setFinder(final BuildNumberFinder finder) {
		this.finder = finder;
	}

	void setVersionBuilderFactory(final VersionBuilderFactory detectorFactory) {
		this.versionBuilderFactory = detectorFactory;
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

		return versionBuilderFactory.newBuilder().setProject(project).setBuildNumber(actualBuildNumber)
				.setUseLastDigitAsBuildNumber(useLastDigitAsBuildNumber).setChangedDependency(changedDependencyOrNull)
				.setRelativePathToModule(relativePathToModuleOrNull).setBusinessVersion(businessVersion).build();
	}
}
