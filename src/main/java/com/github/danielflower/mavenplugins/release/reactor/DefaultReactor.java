package com.github.danielflower.mavenplugins.release.reactor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

final class DefaultReactor implements Reactor {
	private final List<ReleasableModule> modulesInBuildOrder = new LinkedList<ReleasableModule>();
	private final Log log;

	DefaultReactor(final Log log) {
		this.log = log;
	}

	void addReleasableModule(final ReleasableModule module) {
		modulesInBuildOrder.add(module);
	}

	Reactor finalizeReleaseVersions() {
		if (!atLeastOneBeingReleased()) {
			log.warn("No changes have been detected in any modules so will re-release them all");
			final List<ReleasableModule> copy = new ArrayList<>(modulesInBuildOrder);
			modulesInBuildOrder.clear();
			for (final ReleasableModule module : copy) {
				addReleasableModule(module.createReleasableVersion());
			}
		}
		return this;
	}

	private boolean atLeastOneBeingReleased() {
		for (final ReleasableModule module : this) {
			if (module.willBeReleased()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ReleasableModule findByLabel(final String label) {
		for (final ReleasableModule module : modulesInBuildOrder) {
			final String currentLabel = module.getGroupId() + ":" + module.getArtifactId();
			if (currentLabel.equals(label)) {
				return module;
			}
		}
		return null;
	}

	@Override
	public ReleasableModule find(final String groupId, final String artifactId, final String version)
			throws UnresolvedSnapshotDependencyException {
		final ReleasableModule value = findByLabel(groupId + ":" + artifactId);
		if (value == null) {
			throw new UnresolvedSnapshotDependencyException(groupId, artifactId, version);
		}
		return value;
	}

	public String getChangedDependencyOrNull(final MavenProject project) {
		String changedDependency = null;
		for (final ReleasableModule module : this) {
			if (module.willBeReleased()) {
				for (final Dependency dependency : project.getDependencies()) {
					if (dependency.getGroupId().equals(module.getGroupId())
							&& dependency.getArtifactId().equals(module.getArtifactId())) {
						changedDependency = dependency.getArtifactId();
						break;
					}
				}
				if (project.getParent() != null && (project.getParent().getGroupId().equals(module.getGroupId())
						&& project.getParent().getArtifactId().equals(module.getArtifactId()))) {
					changedDependency = project.getParent().getArtifactId();
					break;
				}
			}
			if (changedDependency != null) {
				break;
			}
		}
		return changedDependency;
	}

	@Override
	public Iterator<ReleasableModule> iterator() {
		return modulesInBuildOrder.iterator();
	}
}
