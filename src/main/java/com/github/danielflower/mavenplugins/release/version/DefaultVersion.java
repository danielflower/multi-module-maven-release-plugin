package com.github.danielflower.mavenplugins.release.version;

import org.apache.maven.project.MavenProject;

final class DefaultVersion implements Version {
	private final MavenProject project;
	private final String versionWithoutBuildNumber;
	private final Long buildNumber;

	DefaultVersion(final MavenProject project, final String versionWithoutBuildNumber, final Long buildNumber) {
		this.project = project;
		this.buildNumber = buildNumber;
		this.versionWithoutBuildNumber = versionWithoutBuildNumber;
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
