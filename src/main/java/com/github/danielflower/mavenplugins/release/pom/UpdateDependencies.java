package com.github.danielflower.mavenplugins.release.pom;

import static java.lang.String.format;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.UnresolvedSnapshotDependencyException;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
class UpdateDependencies extends Command {

	@Inject
	UpdateDependencies(final Log log) {
		super(log);
	}

	@Override
	public final void alterModel(final Context updateContext) {
		final MavenProject project = updateContext.getProject();
		final Model originalModel = project.getOriginalModel();
		for (final Dependency dependency : determineDependencies(originalModel)) {
			final String version = dependency.getVersion();
			if (isSnapshot(version)) {
				try {
					final ReleasableModule dependencyBeingReleased = updateContext.getReactor()
							.find(dependency.getGroupId(), dependency.getArtifactId(), version);
					dependency.setVersion(dependencyBeingReleased.getVersionToDependOn());
					log.debug(format(" Dependency on %s rewritten to version %s",
							dependencyBeingReleased.getArtifactId(), dependencyBeingReleased.getVersionToDependOn()));
				} catch (final UnresolvedSnapshotDependencyException e) {
					updateContext.addError("%s references dependency %s %s", project.getArtifactId(), e.artifactId,
							e.version);
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
