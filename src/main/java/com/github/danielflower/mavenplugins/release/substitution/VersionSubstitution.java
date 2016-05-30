package com.github.danielflower.mavenplugins.release.substitution;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

/**
 * Resolves the actual version of an original object (like {@link Dependency} or
 * {@link Plugin} instances). This is necessary if the version of an artifact is
 * specified as property rather than directly declared in the appropriate a
 * &lt;dependency&gt; or &lt;plugin&gt; element. In this case, the
 * &lt;dependency&gt; or &lt;plugin&gt; element of the original model contains a
 * variable like $&#123;fooVersion&#125; which must be substituted before
 * further processing.
 *
 *
 */
public interface VersionSubstitution {

	/**
	 * Determines the actual version of the original {@link Dependency}
	 * specified.
	 * 
	 * @param project
	 *            Maven project; must not be {@code null}
	 * @param originalDependency
	 *            Original dependency, must not be {@code null}
	 * @return Actual version, never {@code null}
	 * @throws IllegalStateException
	 *             Thrown, if no substituted dependency could be found for the
	 *             dependency specified.
	 */
	String getActualVersion(MavenProject project, Dependency originalDependency);

	/**
	 * Determines the actual version of the original {@link Plugin} specified.
	 * 
	 * @param project
	 *            Maven project; must not be {@code null}
	 * @param originalDependency
	 *            Original plugin, must not be {@code null}
	 * @return Actual version, never {@code null}
	 * @throws IllegalStateException
	 *             Thrown, if no substituted dependency could be found for the
	 *             dependency specified.
	 */
	String getActualVersion(MavenProject project, Plugin originalPlugin);
}