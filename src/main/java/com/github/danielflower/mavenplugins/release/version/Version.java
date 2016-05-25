package com.github.danielflower.mavenplugins.release.version;

public interface Version {

	String getReleaseVersion();

	String getBusinessVersion();

	long getBuildNumber();

	/**
	 * The snapshot version, e.g. "1.0-SNAPSHOT"
	 */
	String getDevelopmentVersion();

	boolean useLastDigitAsBuildNumber();
}
