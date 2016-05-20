package com.github.danielflower.mavenplugins.release.version;

public interface Version {

	String releaseVersion();

	String businessVersion();

	long buildNumber();

	/**
	 * The snapshot version, e.g. "1.0-SNAPSHOT"
	 */
	String developmentVersion();
}
