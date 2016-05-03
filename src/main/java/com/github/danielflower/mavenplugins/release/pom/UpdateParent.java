package com.github.danielflower.mavenplugins.release.pom;

import javax.inject.Named;
import javax.inject.Singleton;

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
final class UpdateParent extends Command {

	@Override
	public void alterModel(final Context updateContext) {
		final MavenProject project = updateContext.getProject();
		final Model originalModel = project.getOriginalModel();
		final MavenProject parent = project.getParent();

		if (parent != null && isSnapshot(parent.getVersion())) {
			final String searchingFrom = project.getArtifactId();
			try {
				final ReleasableModule parentBeingReleased = updateContext.getReactor().find(parent.getGroupId(),
						parent.getArtifactId(), parent.getVersion());
				originalModel.getParent().setVersion(parentBeingReleased.getVersionToDependOn());
				updateContext.debug(" Parent %s rewritten to version %s", parentBeingReleased.getArtifactId(),
						parentBeingReleased.getVersionToDependOn());
			} catch (final UnresolvedSnapshotDependencyException e) {
				updateContext.addError("The parent of %s is %s %s", searchingFrom, e.artifactId, e.version);
			}
		}
	}
}