package com.github.danielflower.mavenplugins.release.reactor;

import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.UnresolvedSnapshotDependencyException;

public interface Reactor extends Iterable<ReleasableModule> {

	ReleasableModule findByLabel(String label);

	ReleasableModule find(String groupId, String artifactId, String version)
			throws UnresolvedSnapshotDependencyException;
}
