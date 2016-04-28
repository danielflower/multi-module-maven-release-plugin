package com.github.danielflower.mavenplugins.release.version;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import com.github.danielflower.mavenplugins.release.AnnotatedTagFinder;
import com.github.danielflower.mavenplugins.release.LocalGitRepo;
import com.github.danielflower.mavenplugins.release.ValidationException;

@Named
@Singleton
final class DefaultVersionFactory implements VersionFactory {

	@Override
	public Version newVersioning(final LocalGitRepo gitRepo, final MavenProject project, final Long buildNumber)
			throws MojoExecutionException, ValidationException, GitAPIException {
		final String versionWithoutBuildNumber = project.getVersion().replace("-SNAPSHOT", "");
		final Collection<Long> remoteBuildNumbers = getRemoteBuildNumbers(gitRepo, project, versionWithoutBuildNumber);
		return new DefaultVersion(gitRepo, project, versionWithoutBuildNumber, buildNumber, remoteBuildNumbers);
	}

	private Collection<Long> getRemoteBuildNumbers(final LocalGitRepo gitRepo, final MavenProject project,
			final String versionWithoutBuildNumber) throws GitAPIException {
		final Collection<Ref> remoteTagRefs = gitRepo.allRemoteTags();
		final Collection<Long> remoteBuildNumbers = new ArrayList<Long>();
		final String tagWithoutBuildNumber = project.getArtifactId() + "-" + versionWithoutBuildNumber;
		for (final Ref remoteTagRef : remoteTagRefs) {
			final String remoteTagName = remoteTagRef.getName();
			final Long buildNumber = AnnotatedTagFinder.buildNumberOf(tagWithoutBuildNumber, remoteTagName);
			if (buildNumber != null) {
				remoteBuildNumbers.add(buildNumber);
			}
		}
		return remoteBuildNumbers;
	}
}
