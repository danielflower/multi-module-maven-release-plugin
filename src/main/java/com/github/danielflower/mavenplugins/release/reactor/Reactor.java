package com.github.danielflower.mavenplugins.release.reactor;

public interface Reactor extends Iterable<ReleasableModule> {

	ReleasableModule findByLabel(String label);

	ReleasableModule find(String groupId, String artifactId, String version)
			throws UnresolvedSnapshotDependencyException;
}
