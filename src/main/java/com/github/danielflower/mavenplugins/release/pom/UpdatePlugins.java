package com.github.danielflower.mavenplugins.release.pom;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
public class UpdatePlugins extends Command {

	@Override
	public void alterModel(final Context updateContext) {
		final MavenProject project = updateContext.getProject();
		final String searchingFrom = project.getArtifactId();
		for (final Plugin plugin : project.getModel().getBuild().getPlugins()) {
			final String version = plugin.getVersion();
			if (isSnapshot(version)) {
				if (!isMultiModuleReleasePlugin(plugin)) {
					updateContext.addError("%s references plugin %s %s", searchingFrom, plugin.getArtifactId(),
							version);
				}
			}
		}
	}

	private static boolean isMultiModuleReleasePlugin(final Plugin plugin) {
		return plugin.getGroupId().equals("com.github.danielflower.mavenplugins")
				&& plugin.getArtifactId().equals("multi-module-maven-release-plugin");
	}

}
