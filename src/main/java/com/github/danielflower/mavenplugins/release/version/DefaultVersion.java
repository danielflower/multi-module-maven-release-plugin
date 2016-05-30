package com.github.danielflower.mavenplugins.release.version;

final class DefaultVersion implements Version {
	private String equivalentVersion;
	private String businessVersion;
	private String releaseVersion;
	private long buildNumber;

	void setEquivalentVersion(final String equivalentVersion) {
		this.equivalentVersion = equivalentVersion;
	}

	void setBusinessVersion(final String businessVersion) {
		this.businessVersion = businessVersion;
	}

	void setReleaseVersion(final String releaseVersion) {
		this.releaseVersion = releaseVersion;
	}

	void setBuildNumber(final long buildNumber) {
		this.buildNumber = buildNumber;
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
	 * The business version with the build number appended, e.g. "1.0.1"
	 */
	@Override
	public String getReleaseVersion() {
		return releaseVersion;
	}

	@Override
	public String getEquivalentVersionOrNull() {
		return equivalentVersion;
	}

	@Override
	public void makeReleaseable() {
		setEquivalentVersion(null);
	}
}
