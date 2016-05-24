package com.github.danielflower.mavenplugins.release.pom;

import static java.lang.String.format;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import com.github.danielflower.mavenplugins.release.reactor.ReleasableModule;
import com.github.danielflower.mavenplugins.release.reactor.UnresolvedSnapshotDependencyException;

/**
 * @author rolandhauser
 *
 */
@Component(role = Command.class, hint = "UpdateParent")
final class UpdateParent extends Command {
	static final String ERROR_FORMAT = "The parent of %s is %s %s";

	@Override
	public void alterModel(final Context updateContext) {
		final MavenProject project = updateContext.getProject();
		final Model originalModel = project.getOriginalModel();
		final MavenProject parent = project.getParent();

		if (parent != null && isSnapshot(parent.getVersion())) {
			try {
				final ReleasableModule parentBeingReleased = updateContext.getReactor().find(parent.getGroupId(),
						parent.getArtifactId(), parent.getVersion());
				originalModel.getParent().setVersion(parentBeingReleased.getVersionToDependOn());
				log.debug(format(" Parent %s rewritten to version %s", parent.getArtifactId(),
						parentBeingReleased.getVersionToDependOn()));
			} catch (final UnresolvedSnapshotDependencyException e) {
				updateContext.addError(ERROR_FORMAT, project.getArtifactId(), e.artifactId, e.version);
			}
		}
	}
}