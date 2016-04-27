package com.github.danielflower.mavenplugins.release.reactor;

import com.github.danielflower.mavenplugins.release.LocalGitRepo;
import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.UnresolvedSnapshotDependencyException;

public interface Reactor extends Iterable<ReleasableModule> {

	LocalGitRepo getLocalRepo();

	ReleasableModule findByLabel(String label);

	ReleasableModule find(String groupId, String artifactId, String version)
			throws UnresolvedSnapshotDependencyException;
}
