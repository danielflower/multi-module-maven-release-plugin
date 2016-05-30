package com.github.danielflower.mavenplugins.release.substitution;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;

/**
 * Adapts the properties from the original object.
 *
 *
 * @param <T>
 *            Type of the original object like {@link Dependency} or
 *            {@link Plugin}.
 */
interface PropertyAdapter<T> {

	/**
	 * Returns the {@code artifactId} of the original object.
	 * 
	 * @param origin
	 *            Original object to get the artifactId from; must not be
	 *            {@code null}
	 * @return Origin artifactId, never {@code null}
	 */
	String getArtifactId(T origin);

	/**
	 * Returns the {@code groupId} of the original object.
	 * 
	 * @param origin
	 *            Original object to get the groupId from; must not be
	 *            {@code null}
	 * @return Origin groupId, never {@code null}
	 */
	String getGroupId(T origin);

	/**
	 * Returns the {@code version} of the original object.
	 * 
	 * @param origin
	 *            Original object to get the version from; must not be
	 *            {@code null}
	 * @return Origin version, never {@code null}
	 */
	String getVersion(T origin);
}