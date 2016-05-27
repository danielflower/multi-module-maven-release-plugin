package com.github.danielflower.mavenplugins.release.version;

import static java.lang.Long.valueOf;

import org.apache.maven.project.MavenProject;

/**
 *
 */
final class DefaultVersionBuilder implements VersionBuilder {
	static final String SNAPSHOT_EXTENSION = "-SNAPSHOT";
	private final BuildNumberFinder finder;
	private final ChangeDetectorFactory detectorFactory;
	private MavenProject project;
	private boolean useLastDigitAsBuildNumber;
	private Long buildNumber;
	private String relativePathToModuleOrNull;
	private String changedDependencyOrNull;
	private String remoteUrl;

	DefaultVersionBuilder(final BuildNumberFinder finder, final ChangeDetectorFactory detectorFactory) {
		this.finder = finder;
		this.detectorFactory = detectorFactory;
	}

	@Override
	public VersionBuilder setProject(final MavenProject project) {
		this.project = project;
		return this;
	}

	@Override
	public VersionBuilder setUseLastDigitAsBuildNumber(final boolean useLastDigitAsBuildNumber) {
		this.useLastDigitAsBuildNumber = useLastDigitAsBuildNumber;
		return this;
	}

	@Override
	public VersionBuilder setBuildNumber(final Long buildNumberOrNull) {
		this.buildNumber = buildNumberOrNull;
		return this;
	}

	@Override
	public VersionBuilder setRelativePath(final String relativePathToModuleOrNull) {
		this.relativePathToModuleOrNull = relativePathToModuleOrNull;
		return this;
	}

	@Override
	public VersionBuilder setChangedDependency(final String changedDependencyOrNull) {
		this.changedDependencyOrNull = changedDependencyOrNull;
		return this;
	}

	@Override
	public VersionBuilder setRemoteUrl(final String remoteUrl) {
		this.remoteUrl = remoteUrl;
		return this;
	}

	@Override
	public Version build() throws VersionException {
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
		final DefaultVersion version = new DefaultVersion();
		version.setReleaseVersion(releaseVersion);
		version.setBuildNumber(actualBuildNumber);
		version.setBusinessVersion(businessVersion);
		version.setDevelopmentVersion(useLastDigitAsBuildNumber
				? businessVersion + "." + (actualBuildNumber + 1) + SNAPSHOT_EXTENSION : project.getVersion());
		version.setUseLastDigitAsBuildNumber(useLastDigitAsBuildNumber);
		version.setEquivalentVersion(detectorFactory.newDetector().setProject(project).setBuildNumber(actualBuildNumber)
				.setChangedDependency(changedDependencyOrNull).setRelativePathToModule(relativePathToModuleOrNull)
				.setBusinessVersion(businessVersion).equivalentVersionOrNull());
		return version;
	}

}
