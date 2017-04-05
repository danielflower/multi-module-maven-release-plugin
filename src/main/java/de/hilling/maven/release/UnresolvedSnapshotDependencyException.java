package de.hilling.maven.release;

public class UnresolvedSnapshotDependencyException extends Exception {
    public final String groupId;
    public final String artifactId;

    public UnresolvedSnapshotDependencyException(String groupId, String artifactId) {
        super("Could not find " + groupId + ":" + artifactId);
        this.groupId = groupId;
        this.artifactId = artifactId;
    }
}
