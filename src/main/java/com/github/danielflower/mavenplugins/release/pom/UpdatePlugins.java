package com.github.danielflower.mavenplugins.release.pom;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
public class UpdatePlugins extends Command {

	@Inject
	UpdatePlugins(final Log log) {
		super(log);
	}

	@Override
	public void alterModel(final Context updateContext) {
		final MavenProject project = updateContext.getProject();
		for (final Plugin plugin : project.getModel().getBuild().getPlugins()) {
			final String version = plugin.getVersion();
			if (isSnapshot(version)) {
				if (!isMultiModuleReleasePlugin(plugin)) {
					updateContext.addError("%s references plugin %s %s", project.getArtifactId(),
							plugin.getArtifactId(), version);
				}
			}
		}
	}

	private static boolean isMultiModuleReleasePlugin(final Plugin plugin) {
		return "com.github.danielflower.mavenplugins".equals(plugin.getGroupId())
				&& "multi-module-maven-release-plugin".equals(plugin.getArtifactId());
	}

}
