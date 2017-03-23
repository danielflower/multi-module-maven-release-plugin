package com.github.danielflower.mavenplugins.release;

public class VersionName {
    private final String version;
    private final VersionInfo buildNumber;
    private final String developmentVersion;

    public VersionName(String developmentVersion, String version, VersionInfo buildNumber) {
        Guard.notBlank("developmentVersion", developmentVersion);
        Guard.notBlank("version", version);
        Guard.notNull("buildNumber", buildNumber);
        this.version = version;
        this.buildNumber = buildNumber;
        this.developmentVersion = developmentVersion;
    }

    /**
     * For example, "1.0" if the development version is "1.0-SNAPSHOT"
     */
    public String businessVersion() {
        return version;
    }

    public VersionInfo buildNumber() {
        return buildNumber;
    }

    /**
     * The snapshot version, e.g. "1.0-SNAPSHOT"
     */
    public String developmentVersion() {
        return developmentVersion;
    }

    /**
     * The business version with the build number appended, e.g. "1.0.1"
     */
    public String releaseVersion() {
        return version + "." + buildNumber;
    }
}
