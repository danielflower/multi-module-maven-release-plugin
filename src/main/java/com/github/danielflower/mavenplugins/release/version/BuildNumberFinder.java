package com.github.danielflower.mavenplugins.release.version;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.scm.ProposedTag;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

@Component(role = BuildNumberFinder.class)
class BuildNumberFinder {
	static final String SNAPSHOT_EXTENSION = "-SNAPSHOT";

	@Requirement(role = SCMRepository.class)
	private SCMRepository repository;

	void setRepository(final SCMRepository repository) {
		this.repository = repository;
	}

	public long findBuildNumber(final MavenProject project, final String remoteUrl, final String businessVersion)
			throws MojoExecutionException, ValidationException, GitAPIException {
		final SortedSet<Long> prev = new TreeSet<>();

		for (final ProposedTag previousTag : repository.tagsForVersion(project.getArtifactId(), businessVersion)) {
			prev.add(previousTag.buildNumber());
		}

		prev.addAll(repository.getRemoteBuildNumbers(remoteUrl, project.getArtifactId(), businessVersion));
		return prev.isEmpty() ? 0l : prev.last() + 1;
	}

	public String newBusinessVersion(final MavenProject project, final boolean useLastDigitAsVersionNumber) {
		String businessVersion = project.getVersion().replace(SNAPSHOT_EXTENSION, "");
		if (useLastDigitAsVersionNumber) {
			businessVersion = businessVersion.substring(0, businessVersion.lastIndexOf('.'));
		}
		return businessVersion;
	}
}
