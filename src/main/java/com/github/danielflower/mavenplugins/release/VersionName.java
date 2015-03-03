package com.github.danielflower.mavenplugins.release;

public class VersionName {
    private final String version;
    private final String buildNumber;
    private final String developmentVersion;

    public VersionName(String developmentVersion, String version, String buildNumber) {
        Guard.notBlank("developmentVersion", developmentVersion);
        Guard.notBlank("version", version);
        Guard.notBlank("buildNumber", buildNumber);
        this.version = version;
        this.buildNumber = buildNumber;
        this.developmentVersion = developmentVersion;
    }

    public String version() {
        return version;
    }

    public String buildNumber() {
        return buildNumber;
    }

    public String developmentVersion() {
        return developmentVersion;
    }

    public String fullVersion() {
        return version + "." + buildNumber;
    }
}
