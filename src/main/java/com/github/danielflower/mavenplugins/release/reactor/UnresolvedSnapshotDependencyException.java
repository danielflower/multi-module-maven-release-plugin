package com.github.danielflower.mavenplugins.release.reactor;

@SuppressWarnings("serial")
public class UnresolvedSnapshotDependencyException extends ReactorException {
	public final String groupId;
	public final String artifactId;

	public UnresolvedSnapshotDependencyException(final String groupId, final String artifactId) {
		super("Could not find %s:%s", groupId, artifactId);
		this.groupId = groupId;
		this.artifactId = artifactId;
	}
}
