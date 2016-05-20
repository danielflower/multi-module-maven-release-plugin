package com.github.danielflower.mavenplugins.release.version;

import static org.apache.commons.lang.StringUtils.EMPTY;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

/**
 * Default implementation of the {@link VersionFactory} interface.
 *
 */
@Component(role = VersionFactory.class)
final class DefaultVersionFactory implements VersionFactory {
	static final String SNAPSHOT_EXTENSION = "-SNAPSHOT";

	@Requirement(role = SCMRepository.class)
	private SCMRepository repository;

	@Requirement(role = BuildNumberFinder.class)
	private BuildNumberFinder finder;

	void setRepository(final SCMRepository repository) {
		this.repository = repository;
	}

	void setFinder(final BuildNumberFinder finder) {
		this.finder = finder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.danielflower.mavenplugins.release.version.VersionFactory#
	 * newVersioning(org.apache.maven.project.MavenProject, java.lang.Long,
	 * java.lang.String)
	 */
	@Override
	public Version newVersion(final MavenProject project, final Long buildNumberOrNull, final String remoteUrl)
			throws MojoExecutionException, ValidationException, GitAPIException {
		final String businessVersion = project.getVersion().replace(SNAPSHOT_EXTENSION, EMPTY);

		final long actualBuildNumber = buildNumberOrNull == null ? finder.findBuildNumber(project, remoteUrl, businessVersion)
				: buildNumberOrNull;

		return new DefaultVersion(project.getVersion(), businessVersion, actualBuildNumber);
	}
}
