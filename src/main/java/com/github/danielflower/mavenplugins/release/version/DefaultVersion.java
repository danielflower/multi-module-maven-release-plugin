package com.github.danielflower.mavenplugins.release.version;

final class DefaultVersion implements Version {
	private final String developmentVersion;
	private final String businessVersion;
	private final String releaseVersion;
	private final long buildNumber;

	DefaultVersion(final String developmentVersion, final String businessVersion, final long buildNumber,
			final boolean useLastDigitAsBuildNumber) {
		this.buildNumber = buildNumber;
		this.businessVersion = businessVersion;
		this.releaseVersion = businessVersion + "." + buildNumber;
		this.developmentVersion = useLastDigitAsBuildNumber ? businessVersion + "." + (buildNumber + 1) + "-SNAPSHOT"
				: developmentVersion;
	}

	/**
	 * For example, "1.0" if the development version is "1.0-SNAPSHOT"
	 */
	@Override
	public String getBusinessVersion() {
		return businessVersion;
	}

	@Override
	public long getBuildNumber() {
		return buildNumber;
	}

	/**
	 * The snapshot version, e.g. "1.0-SNAPSHOT"
	 */
	@Override
	public String getDevelopmentVersion() {
		return developmentVersion;
	}

	/**
	 * The business version with the build number appended, e.g. "1.0.1"
	 */
	@Override
	public String getReleaseVersion() {
		return releaseVersion;
	}
}
