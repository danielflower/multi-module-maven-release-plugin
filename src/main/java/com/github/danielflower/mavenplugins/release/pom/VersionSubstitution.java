package com.github.danielflower.mavenplugins.release.pom;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = VersionSubstitution.class)
class VersionSubstitution {

	private static interface Artifact {

		String getArtifactId();

		String getGroupId();

		String getVersion();
	}

	private static interface Converter<T> {

		String getArtifactId(T origin);

		String getGroupId(T origin);

		String getVersion(T origin);
	}

	private <T> List<Artifact> convert(final List<T> origins, final Converter<T> converter) {
		final List<Artifact> artifacts = new LinkedList<>();
		for (final T origin : origins) {
			artifacts.add(new Artifact() {

				@Override
				public String getArtifactId() {
					return converter.getArtifactId(origin);
				}

				@Override
				public String getGroupId() {
					return converter.getGroupId(origin);
				}

				@Override
				public String getVersion() {
					return converter.getVersion(origin);
				}

			});
		}

		return artifacts;
	}

	private <T> String getSubstitutedVersionOrNull(final List<T> origins, final T origin,
			final Converter<T> converter) {
		for (final Artifact substituted : convert(origins, converter)) {
			if (converter.getGroupId(origin).equals(substituted.getGroupId())
					&& converter.getArtifactId(origin).equals(substituted.getArtifactId())) {
				return substituted.getVersion();
			}
		}
		return null;
	}

	String getSubstitutedDependencyVersionOrNull(final MavenProject project, final Dependency originalDependency) {
		return getSubstitutedVersionOrNull(project.getDependencies(), originalDependency, new Converter<Dependency>() {

			@Override
			public String getArtifactId(final Dependency origin) {
				return origin.getArtifactId();
			}

			@Override
			public String getGroupId(final Dependency origin) {
				return origin.getGroupId();
			}

			@Override
			public String getVersion(final Dependency origin) {
				return origin.getVersion();
			}

		});
	}

	String getSubstitutedPluginVersionOrNull(final MavenProject project, final Plugin originalPlugin) {
		return getSubstitutedVersionOrNull(project.getBuildPlugins(), originalPlugin, new Converter<Plugin>() {

			@Override
			public String getArtifactId(final Plugin origin) {
				return origin.getArtifactId();
			}

			@Override
			public String getGroupId(final Plugin origin) {
				return origin.getGroupId();
			}

			@Override
			public String getVersion(final Plugin origin) {
				return origin.getVersion();
			}

		});
	}
}
