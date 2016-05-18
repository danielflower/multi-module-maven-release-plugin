package com.github.danielflower.mavenplugins.release.substitution;

import static java.lang.String.format;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Default implementation of the {@link VersionSubstitution} interface.
 *
 */
@Component(role = VersionSubstitution.class)
class DefaultVersionSubstitution implements VersionSubstitution {

	@Requirement(role = PropertyAdapter.class, hint = "dependencyAdapter")
	private PropertyAdapter<Dependency> dependencyAdapter;

	@Requirement(role = PropertyAdapter.class, hint = "pluginAdapter")
	private PropertyAdapter<Plugin> pluginAdapter;

	void setDependencyAdapter(final PropertyAdapter<Dependency> dependencyAdapter) {
		this.dependencyAdapter = dependencyAdapter;
	}

	void setPluginAdapter(final PropertyAdapter<Plugin> pluginAdapter) {
		this.pluginAdapter = pluginAdapter;
	}

	/**
	 * Converts a list of original objects (like {@link Dependency} or
	 * {@link Plugin} objects) to a new list of appropriate {@link Artifact}
	 * instances.
	 * 
	 * @param origins
	 *            List of original objects, must not be {@code null}
	 * @param adapter
	 *            Adapter to convert an original object into an {@link Artifact}
	 *            instance; must not be {@code null}.
	 * @return
	 */
	private <T> List<Artifact> convert(final List<T> origins, final PropertyAdapter<T> adapter) {
		final List<Artifact> artifacts = new LinkedList<>();
		for (final T origin : origins) {
			artifacts.add(new Artifact() {

				@Override
				public String getArtifactId() {
					return adapter.getArtifactId(origin);
				}

				@Override
				public String getGroupId() {
					return adapter.getGroupId(origin);
				}

				@Override
				public String getVersion() {
					return adapter.getVersion(origin);
				}

			});
		}

		return artifacts;
	}

	/**
	 * @param substituted
	 * @param origin
	 * @param adapter
	 * @return
	 */
	private <T> String getActualVersion(final List<T> substituted, final T origin,
			final PropertyAdapter<T> adapter) {
		for (final Artifact artifact : convert(substituted, adapter)) {
			if (adapter.getGroupId(origin).equals(artifact.getGroupId())
					&& adapter.getArtifactId(origin).equals(artifact.getArtifactId())) {
				return artifact.getVersion();
			}
		}
		throw new IllegalStateException(format("No matching substituted object found for %s", origin));
	}

	@Override
	public String getActualVersion(final MavenProject project, final Dependency originalDependency) {
		return getActualVersion(project.getDependencies(), originalDependency, dependencyAdapter);
	}

	@Override
	public String getActualVersion(final MavenProject project, final Plugin originalPlugin) {
		return getActualVersion(project.getBuildPlugins(), originalPlugin, pluginAdapter);
	}
}
