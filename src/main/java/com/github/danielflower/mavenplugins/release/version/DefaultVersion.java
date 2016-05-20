package com.github.danielflower.mavenplugins.release.version;

final class DefaultVersion implements Version {
	private final String developmentVersion;
	private final String versionWithoutBuildNumber;
	private final long buildNumber;

	DefaultVersion(final String developmentVersion, final String businessVersion, final long buildNumber) {
		this.developmentVersion = developmentVersion;
		this.buildNumber = buildNumber;
		this.versionWithoutBuildNumber = businessVersion;
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
		return developmentVersion;
	}

	/**
	 * The business version with the build number appended, e.g. "1.0.1"
	 */
	@Override
	public String releaseVersion() {
		return businessVersion() + "." + buildNumber;
	}
}
