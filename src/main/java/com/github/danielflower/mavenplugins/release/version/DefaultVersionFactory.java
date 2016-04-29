package com.github.danielflower.mavenplugins.release.version;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

@Named
@Singleton
final class DefaultVersionFactory implements VersionFactory {
	private final SCMRepository repository;

	@Inject
	DefaultVersionFactory(final SCMRepository repository) {
		this.repository = repository;
	}

	@Override
	public Version newVersioning(final MavenProject project, final Long buildNumber)
			throws MojoExecutionException, ValidationException, GitAPIException {
		final String versionWithoutBuildNumber = project.getVersion().replace("-SNAPSHOT", "");
		return new DefaultVersion(repository, project, versionWithoutBuildNumber, buildNumber);
	}
}
