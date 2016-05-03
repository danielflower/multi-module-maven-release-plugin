package com.github.danielflower.mavenplugins.release.pom;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.UnresolvedSnapshotDependencyException;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
final class UpdateDependencies extends Command {

	@Override
	public void alterModel(final Context updateContext) {
		final MavenProject project = updateContext.getProject();
		final Model originalModel = project.getOriginalModel();
		for (final Dependency dependency : originalModel.getDependencies()) {
			final String version = dependency.getVersion();
			if (isSnapshot(version)) {
				final String searchingFrom = project.getArtifactId();
				try {
					final ReleasableModule dependencyBeingReleased = updateContext.getReactor()
							.find(dependency.getGroupId(), dependency.getArtifactId(), version);
					dependency.setVersion(dependencyBeingReleased.getVersionToDependOn());
					updateContext.debug(" Dependency on %s rewritten to version %s",
							dependencyBeingReleased.getArtifactId(), dependencyBeingReleased.getVersionToDependOn());
				} catch (final UnresolvedSnapshotDependencyException e) {
					updateContext.addError("%s references dependency %s %s", searchingFrom, e.artifactId, e.version);
				}
			} else {
				updateContext.debug(" Dependency on %s kept at version %s", dependency.getArtifactId(),
						dependency.getVersion());
			}
		}
	}
}
