package com.github.danielflower.mavenplugins.release;

import org.apache.commons.lang3.StringUtils;

public class VersionName {
    private final String version;
    private final String buildNumber;
    private final String developmentVersion;
    private final String delimiter;
    private final String releaseVersion;

    public VersionName(String developmentVersion, String version, String buildNumber, String delimiter) {
        Guard.notBlank("developmentVersion", developmentVersion);
        Guard.notBlank("version", version);
        Guard.notNull("buildNumber", buildNumber);
        this.version = version;
        this.buildNumber = buildNumber;
        this.developmentVersion = developmentVersion;
        this.delimiter = delimiter;
        this.releaseVersion = buildNumber.length() > 0 ? version + delimiter + buildNumber : version;
    }

    public VersionName(String developmentVersion, String version, String buildNumber) {
        this(developmentVersion, version, buildNumber, ".");
    }
    
    /**
     * For example, "1.0" if the development version is "1.0-SNAPSHOT".
     */
    public String businessVersion() {
        return version;
    }

    public String buildNumber() {
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
        return releaseVersion;
    }
}
