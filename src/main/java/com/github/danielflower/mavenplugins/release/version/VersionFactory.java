package com.github.danielflower.mavenplugins.release.version;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.ValidationException;

/**
 * Factory interface for creating new {@link Version} instances.
 */
public interface VersionFactory {

	Version newVersion(MavenProject project, boolean useLastDigitAsVersionNumber, Long buildNumberOrNull,
			String remoteUrl) throws MojoExecutionException, ValidationException, GitAPIException;
}
