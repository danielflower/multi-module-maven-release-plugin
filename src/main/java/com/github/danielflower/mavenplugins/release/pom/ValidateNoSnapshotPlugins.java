package com.github.danielflower.mavenplugins.release.pom;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Validates that POM to be release does not refer a plugin with a snapshot
 * version. The only exception is the multi-module-maven-release-plugin itself
 * (necessary for testing).
 *
 */
@Named
@Singleton
public class ValidateNoSnapshotPlugins extends Command {
	static final String ERROR_FORMAT = "%s references plugin %s %s";
	static final String MULTI_MODULE_MAVEN_PLUGIN_GROUP_ID = "com.github.danielflower.mavenplugins";
	static final String MULTI_MODULE_MAVEN_PLUGIN_ARTIFACT_ID = "multi-module-maven-release-plugin";

	@com.google.inject.Inject // Compatibility: Maven 3.0.1 - 3.2.1
	@Inject // Maven 3.3.0 and greater
	ValidateNoSnapshotPlugins(final Log log) {
		super(log);
	}

	@Override
	public void alterModel(final Context updateContext) {
		final MavenProject project = updateContext.getProject();
		for (final Plugin plugin : project.getModel().getBuild().getPlugins()) {
			final String version = plugin.getVersion();
			if (isSnapshot(version) && !isMultiModuleReleasePlugin(plugin)) {
				updateContext.addError(ERROR_FORMAT, project.getArtifactId(), plugin.getArtifactId(), version);
			}
		}
	}

	private static boolean isMultiModuleReleasePlugin(final Plugin plugin) {
		return MULTI_MODULE_MAVEN_PLUGIN_GROUP_ID.equals(plugin.getGroupId())
				&& MULTI_MODULE_MAVEN_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId());
	}
}
