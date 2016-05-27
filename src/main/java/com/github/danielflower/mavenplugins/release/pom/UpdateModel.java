package com.github.danielflower.mavenplugins.release.pom;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import com.github.danielflower.mavenplugins.release.reactor.ReleasableModule;
import com.github.danielflower.mavenplugins.release.reactor.UnresolvedSnapshotDependencyException;

@Component(role = Command.class, hint = "UpdateModel")
final class UpdateModel extends Command {
	static final String ERROR_FORMAT = "Project not found in reactor: %s";

	@Override
	public void alterModel(final Context updateContext) {
		final MavenProject project = updateContext.getProject();
		final Model originalModel = project.getOriginalModel();
		try {
			final ReleasableModule module = updateContext.getVersionToDependOn(project.getGroupId(),
					project.getArtifactId());
			originalModel.setVersion(module.getVersionToDependOn());
		} catch (final UnresolvedSnapshotDependencyException e) {
			updateContext.addError(ERROR_FORMAT, project);
		}
	}
}
