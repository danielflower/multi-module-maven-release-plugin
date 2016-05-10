package com.github.danielflower.mavenplugins.release.version;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.ValidationException;

public interface VersionFactory {

	Version newVersioning(MavenProject project, Long buildNumber, String remoteUrl)
			throws MojoExecutionException, ValidationException, GitAPIException;
}
