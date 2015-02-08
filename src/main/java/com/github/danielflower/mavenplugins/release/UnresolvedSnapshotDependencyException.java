package com.github.danielflower.mavenplugins.release;

public class UnresolvedSnapshotDependencyException extends Exception {
    public final String groupId;
    public final String artifactId;
    public final String version;

    public UnresolvedSnapshotDependencyException(String groupId, String artifactId, String version) {
        super("Could not find " + groupId + ":" + artifactId + ":" + version);
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }
}
