package com.github.danielflower.mavenplugins.release.reactor;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Repository;

import com.github.danielflower.mavenplugins.release.version.VersionBuilder;
import com.github.danielflower.mavenplugins.release.version.VersionBuilderFactory;
import com.github.danielflower.mavenplugins.release.version.VersionException;

final class DefaultReactorBuilder implements ReactorBuilder {
	private final Log log;
	private final VersionBuilderFactory versionBuilderFactory;
	private MavenProject rootProject;
	private List<MavenProject> projects;
	private boolean useLastDigitAsBuildNumber;
	private Long buildNumber;
	private List<String> modulesToForceRelease;
	private String remoteUrl;

	public DefaultReactorBuilder(final Log log, final VersionBuilderFactory versioningFactory) {
		this.log = log;
		this.versionBuilderFactory = versioningFactory;
	}

	@Override
	public ReactorBuilder setRootProject(final MavenProject rootProject) {
		this.rootProject = rootProject;
		return this;
	}

	@Override
	public ReactorBuilder setProjects(final List<MavenProject> projects) {
		this.projects = projects;
		return this;
	}

	@Override
	public ReactorBuilder setBuildNumber(final Long buildNumber) {
		this.buildNumber = buildNumber;
		return this;
	}

	@Override
	public ReactorBuilder setUseLastDigitAsBuildNumber(final boolean useLastDigitAsBuildNumber) {
		this.useLastDigitAsBuildNumber = useLastDigitAsBuildNumber;
		return this;
	}

	@Override
	public ReactorBuilder setModulesToForceRelease(final List<String> modulesToForceRelease) {
		this.modulesToForceRelease = modulesToForceRelease;
		return this;
	}

	@Override
	public Reactor build() throws ReactorException {
		final DefaultReactor reactor = new DefaultReactor(log);

		for (final MavenProject project : projects) {
			try {
				final String relativePathToModule = calculateModulePath(rootProject, project);
				final String changedDependencyOrNull = reactor.getChangedDependencyOrNull(project);
				final VersionBuilder versionBuilder = versionBuilderFactory.newBuilder();
				versionBuilder.setProject(project);
				versionBuilder.setUseLastDigitAsBuildNumber(useLastDigitAsBuildNumber);
				versionBuilder.setBuildNumber(buildNumber);
				versionBuilder.setChangedDependency(changedDependencyOrNull);
				versionBuilder.setRemoteUrl(remoteUrl);

				if (modulesToForceRelease == null || !modulesToForceRelease.contains(project.getArtifactId())) {
					versionBuilder.setRelativePath(relativePathToModule);
				}

				reactor.addReleasableModule(
						new ReleasableModule(project, versionBuilder.build(), relativePathToModule));
			} catch (final VersionException e) {
				throw new ReactorException(e, "Version could be created for project %s", project);
			}
		}

		return reactor.finalizeReleaseVersions();
	}

	private static String calculateModulePath(final MavenProject rootProject, final MavenProject project)
			throws ReactorException {
		// Getting canonical files because on Windows, it's possible one returns
		// "C:\..." and the other "c:\..." which is rather amazing
		File projectRoot;
		File moduleRoot;
		try {
			projectRoot = rootProject.getBasedir().getCanonicalFile();
			moduleRoot = project.getBasedir().getCanonicalFile();
		} catch (final IOException e) {
			throw new ReactorException(e, "Could not find directory paths for maven project");
		}
		String relativePathToModule = Repository.stripWorkDir(projectRoot, moduleRoot);
		if (relativePathToModule.length() == 0) {
			relativePathToModule = ".";
		}
		return relativePathToModule;
	}

	@Override
	public ReactorBuilder setRemoteUrl(final String remoteUrl) {
		this.remoteUrl = remoteUrl;
		return this;
	}
}
