package com.github.danielflower.mavenplugins.release.version;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

@Component(role = VersionFactory.class)
final class DefaultVersionFactory implements VersionFactory {

	@Requirement(role = SCMRepository.class)
	private SCMRepository repository;

	void setRepository(final SCMRepository repository) {
		this.repository = repository;
	}

	@Override
	public Version newVersioning(final MavenProject project, final Long buildNumber, final String remoteUrl)
			throws MojoExecutionException, ValidationException, GitAPIException {
		final String versionWithoutBuildNumber = project.getVersion().replace("-SNAPSHOT", "");
		return new DefaultVersion(repository, project, versionWithoutBuildNumber, buildNumber, remoteUrl);
	}
}
