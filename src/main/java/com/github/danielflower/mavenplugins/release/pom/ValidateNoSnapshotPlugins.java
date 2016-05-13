package com.github.danielflower.mavenplugins.release.pom;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Validates that POM to be release does not refer a plugin with a snapshot
 * version. The only exception is the multi-module-maven-release-plugin itself
 * (necessary for testing).
 *
 */
@Component(role = Command.class, hint = "ValidateNoSnapshotPlugins")
public class ValidateNoSnapshotPlugins extends Command {
	static final String ERROR_FORMAT = "%s references plugin %s %s";
	static final String MULTI_MODULE_MAVEN_PLUGIN_GROUP_ID = "com.github.danielflower.mavenplugins";
	static final String MULTI_MODULE_MAVEN_PLUGIN_ARTIFACT_ID = "multi-module-maven-release-plugin";

	@Requirement(role = VersionSubstitution.class)
	private VersionSubstitution substitution;

	void setVersionSubstitution(final VersionSubstitution substitution) {
		this.substitution = substitution;
	}

	@Override
	public void alterModel(final Context updateContext) {
		final MavenProject project = updateContext.getProject();
		for (final Plugin plugin : project.getModel().getBuild().getPlugins()) {
			final String version = substitution.getSubstitutedPluginVersionOrNull(project, plugin);
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
