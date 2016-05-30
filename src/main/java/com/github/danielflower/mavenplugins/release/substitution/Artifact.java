package com.github.danielflower.mavenplugins.release.substitution;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;

/**
 * A view on an original object like {@link Dependency} or {@link Plugin}
 * which provides a {@code groupId}, {@code artifactId} and {@code version}.
 *
 */
interface Artifact {

	/**
	 * Returns the {@code artifactId} of the original object.
	 * 
	 * @return artifactId, never {@code null}
	 */
	String getArtifactId();

	/**
	 * Returns the {@code groupId} of the original object.
	 * 
	 * @return groupId, never {@code null}
	 */
	String getGroupId();

	/**
	 * Returns the actual version {@code groupId} of the original object.
	 * 
	 * @return groupId, never {@code null}
	 */
	String getVersion();
}