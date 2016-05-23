package com.github.danielflower.mavenplugins.release.reactor;

@SuppressWarnings("serial")
public class UnresolvedSnapshotDependencyException extends ReactorException {
	public final String groupId;
	public final String artifactId;
	public final String version;

	public UnresolvedSnapshotDependencyException(final String groupId, final String artifactId, final String version) {
		super("Could not find %s:%s:%s", groupId, artifactId, version);
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}
}
