package com.github.danielflower.mavenplugins.release.pom;

import static java.lang.String.format;

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.github.danielflower.mavenplugins.release.reactor.UnresolvedSnapshotDependencyException;
import com.github.danielflower.mavenplugins.release.substitution.VersionSubstitution;

/**
 * @author rolandhauser
 *
 */
@Component(role = Command.class, hint = "UpdateDependencies")
class UpdateDependencies extends Command {
	static final String ERROR_FORMAT = "%s references dependency %s %s";

	@Requirement(role = VersionSubstitution.class)
	private VersionSubstitution substitution;

	void setVersionSubstitution(final VersionSubstitution substitution) {
		this.substitution = substitution;
	}

	@Override
	public final void alterModel(final Context updateContext) {
		final MavenProject project = updateContext.getProject();
		final Model originalModel = project.getOriginalModel();

		for (final Dependency dependency : determineDependencies(originalModel)) {
			final String version = substitution.getActualVersion(project, dependency);
			if (isSnapshot(version)) {
				try {
					final String versionToDependOn = updateContext.getVersionToDependOn(dependency.getGroupId(),
							dependency.getArtifactId(), version);
					dependency.setVersion(versionToDependOn);
					log.debug(format(" Dependency on %s rewritten to version %s", dependency.getArtifactId(),
							versionToDependOn));
				} catch (final UnresolvedSnapshotDependencyException e) {
					updateContext.addError(ERROR_FORMAT, project.getArtifactId(), e.artifactId, e.version);
				}
			} else {
				log.debug(format(" Dependency on %s kept at version %s", dependency.getArtifactId(),
						dependency.getVersion()));
			}
		}
	}

	protected List<Dependency> determineDependencies(final Model originalModel) {
		return originalModel.getDependencies();
	}
}
