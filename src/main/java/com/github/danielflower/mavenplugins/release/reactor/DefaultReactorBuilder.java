package com.github.danielflower.mavenplugins.release.reactor;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import com.github.danielflower.mavenplugins.release.AnnotatedTag;
import com.github.danielflower.mavenplugins.release.LocalGitRepo;
import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.version.Version;
import com.github.danielflower.mavenplugins.release.version.VersionFactory;

final class DefaultReactorBuilder implements ReactorBuilder {
	private final VersionFactory versionFactory;
	private Log log;
	private LocalGitRepo gitRepo;
	private MavenProject rootProject;
	private List<MavenProject> projects;
	private Long buildNumber;
	private List<String> modulesToForceRelease;

	public DefaultReactorBuilder(final VersionFactory versioningFactory) {
		this.versionFactory = versioningFactory;
	}

	@Override
	public ReactorBuilder setLog(final Log log) {
		this.log = log;
		return this;
	}

	@Override
	public ReactorBuilder setGitRepo(final LocalGitRepo gitRepo) {
		this.gitRepo = gitRepo;
		return this;
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
	public ReactorBuilder setModulesToForceRelease(final List<String> modulesToForceRelease) {
		this.modulesToForceRelease = modulesToForceRelease;
		return this;
	}

	private String getChangedDependencyOrNull(final Reactor reactor, final MavenProject project) {
		boolean oneOfTheDependenciesHasChanged = false;
		String changedDependency = null;
		for (final ReleasableModule module : reactor) {
			if (module.willBeReleased()) {
				for (final Dependency dependency : project.getModel().getDependencies()) {
					if (dependency.getGroupId().equals(module.getGroupId())
							&& dependency.getArtifactId().equals(module.getArtifactId())) {
						oneOfTheDependenciesHasChanged = true;
						changedDependency = dependency.getArtifactId();
						break;
					}
				}
				if (project.getParent() != null && (project.getParent().getGroupId().equals(module.getGroupId())
						&& project.getParent().getArtifactId().equals(module.getArtifactId()))) {
					oneOfTheDependenciesHasChanged = true;
					changedDependency = project.getParent().getArtifactId();
					break;
				}
			}
			if (oneOfTheDependenciesHasChanged) {
				break;
			}
		}
		return changedDependency;
	}

	@Override
	public Reactor build() throws ValidationException, GitAPIException, MojoExecutionException {
		final DefaultReactor reactor = new DefaultReactor(log, gitRepo);

		for (final MavenProject project : projects) {
			final String artifactId = project.getArtifactId();

			final Version version = versionFactory.newVersioning(gitRepo, project,
					buildNumber);

			final String changedDependencyOrNull = getChangedDependencyOrNull(reactor, project);
			final String relativePathToModule = calculateModulePath(rootProject, project);

			final String equivalentVersion = logReleaseInfo(artifactId, version, changedDependencyOrNull,
					relativePathToModule);

			reactor.addReleasableModule(
					new ReleasableModule(project, version, equivalentVersion, relativePathToModule));
		}

		return reactor.finalizeReleaseVersions();
	}

	private String logReleaseInfo(final String artifactId, final Version versioning,
			final String changedDependencyOrNull, final String relativePathToModule) throws MojoExecutionException {
		String equivalentVersion = null;
		if (modulesToForceRelease != null && modulesToForceRelease.contains(artifactId)) {
			log.info(format("Releasing %s %s as we was asked to forced release.", artifactId,
					versioning.releaseVersion()));
		} else if (changedDependencyOrNull != null) {
			log.info(format("Releasing %s %s as %s has changed.", artifactId, versioning.releaseVersion(),
					changedDependencyOrNull));
		} else {
			final AnnotatedTag previousTagThatIsTheSameAsHEADForThisModule = versioning
					.hasChangedSinceLastRelease(relativePathToModule);

			if (previousTagThatIsTheSameAsHEADForThisModule != null) {
				equivalentVersion = previousTagThatIsTheSameAsHEADForThisModule.version() + "."
						+ previousTagThatIsTheSameAsHEADForThisModule.buildNumber();
				log.info(format("Will use version %s for %s as it has not been changed since that release.",
						equivalentVersion, artifactId));
			} else {
				log.info(format("Will use version %s for %s as it has changed since the last release.",
						versioning.releaseVersion(), artifactId));
			}
		}
		return equivalentVersion;
	}

	private static String calculateModulePath(final MavenProject rootProject, final MavenProject project)
			throws MojoExecutionException {
		// Getting canonical files because on Windows, it's possible one returns
		// "C:\..." and the other "c:\..." which is rather amazing
		File projectRoot;
		File moduleRoot;
		try {
			projectRoot = rootProject.getBasedir().getCanonicalFile();
			moduleRoot = project.getBasedir().getCanonicalFile();
		} catch (final IOException e) {
			throw new MojoExecutionException("Could not find directory paths for maven project", e);
		}
		String relativePathToModule = Repository.stripWorkDir(projectRoot, moduleRoot);
		if (relativePathToModule.length() == 0) {
			relativePathToModule = ".";
		}
		return relativePathToModule;
	}
}
