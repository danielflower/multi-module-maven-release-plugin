package com.github.danielflower.mavenplugins.release.reactor;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.ValidationException;

public interface ReactorBuilder {

	ReactorBuilder setRootProject(MavenProject rootProject);

	ReactorBuilder setProjects(List<MavenProject> projects);

	ReactorBuilder setBuildNumber(final Long buildNumber);

	ReactorBuilder setModulesToForceRelease(final List<String> modulesToForceRelease);

	ReactorBuilder setRemoteUrl(String remoteUrl);

	Reactor build() throws ValidationException, GitAPIException, MojoExecutionException;
}
