package com.github.danielflower.mavenplugins.release.reactor;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import com.github.danielflower.mavenplugins.release.AnnotatedTag;
import com.github.danielflower.mavenplugins.release.AnnotatedTagFinder;
import com.github.danielflower.mavenplugins.release.DiffDetector;
import com.github.danielflower.mavenplugins.release.LocalGitRepo;
import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.TreeWalkingDiffDetector;
import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.VersionName;
import com.github.danielflower.mavenplugins.release.VersionNamer;

final class DefaultReactorBuilder implements ReactorBuilder {
	private Log log;
	private LocalGitRepo gitRepo;
	private MavenProject rootProject;
	private List<MavenProject> projects;
	private Long buildNumber;
	private List<String> modulesToForceRelease;

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

	@Override
	public Reactor build() throws ValidationException, GitAPIException, MojoExecutionException {
		final DefaultReactor reactor = new DefaultReactor(log, gitRepo);
		final DiffDetector detector = new TreeWalkingDiffDetector(gitRepo.git.getRepository());

		final VersionNamer versionNamer = new VersionNamer();
		for (final MavenProject project : projects) {
			final String relativePathToModule = calculateModulePath(rootProject, project);
			final String artifactId = project.getArtifactId();
			final String versionWithoutBuildNumber = project.getVersion().replace("-SNAPSHOT", "");
			final List<AnnotatedTag> previousTagsForThisModule = AnnotatedTagFinder.tagsForVersion(gitRepo.git,
					artifactId, versionWithoutBuildNumber);

			final Collection<Long> previousBuildNumbers = new ArrayList<Long>();
			if (previousTagsForThisModule != null) {
				for (final AnnotatedTag previousTag : previousTagsForThisModule) {
					previousBuildNumbers.add(previousTag.buildNumber());
				}
			}

			final Collection<Long> remoteBuildNumbers = getRemoteBuildNumbers(artifactId, versionWithoutBuildNumber);
			previousBuildNumbers.addAll(remoteBuildNumbers);

			final VersionName newVersion = versionNamer.name(project.getVersion(), buildNumber, previousBuildNumbers);

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

			String equivalentVersion = null;

			if (modulesToForceRelease != null && modulesToForceRelease.contains(artifactId)) {
				log.info(format("Releasing %s %s as we was asked to forced release.", artifactId,
						newVersion.releaseVersion()));
			} else if (oneOfTheDependenciesHasChanged) {
				log.info(format("Releasing %s %s as %s has changed.", artifactId, newVersion.releaseVersion(),
						changedDependency));
			} else {
				final AnnotatedTag previousTagThatIsTheSameAsHEADForThisModule = hasChangedSinceLastRelease(
						previousTagsForThisModule, detector, project, relativePathToModule);
				if (previousTagThatIsTheSameAsHEADForThisModule != null) {
					equivalentVersion = previousTagThatIsTheSameAsHEADForThisModule.version() + "."
							+ previousTagThatIsTheSameAsHEADForThisModule.buildNumber();
					log.info(format("Will use version %s for %s as it has not been changed since that release.",
							equivalentVersion, artifactId));
				} else {
					log.info(format("Will use version %s for %s as it has changed since the last release.",
							newVersion.releaseVersion(), artifactId));
				}
			}

			reactor.addReleasableModule(
					new ReleasableModule(project, newVersion, equivalentVersion, relativePathToModule));
		}

		return reactor.finalizeReleaseVersions();
	}

	private Collection<Long> getRemoteBuildNumbers(final String artifactId, final String versionWithoutBuildNumber)
			throws GitAPIException {
		final Collection<Ref> remoteTagRefs = gitRepo.allRemoteTags();
		final Collection<Long> remoteBuildNumbers = new ArrayList<Long>();
		final String tagWithoutBuildNumber = artifactId + "-" + versionWithoutBuildNumber;
		for (final Ref remoteTagRef : remoteTagRefs) {
			final String remoteTagName = remoteTagRef.getName();
			final Long buildNumber = AnnotatedTagFinder.buildNumberOf(tagWithoutBuildNumber, remoteTagName);
			if (buildNumber != null) {
				remoteBuildNumbers.add(buildNumber);
			}
		}
		return remoteBuildNumbers;
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

	static AnnotatedTag hasChangedSinceLastRelease(final List<AnnotatedTag> previousTagsForThisModule,
			final DiffDetector detector, final MavenProject project, final String relativePathToModule)
					throws MojoExecutionException {
		try {
			if (previousTagsForThisModule.size() == 0)
				return null;
			final boolean hasChanged = detector.hasChangedSince(relativePathToModule, project.getModel().getModules(),
					previousTagsForThisModule);
			return hasChanged ? null : tagWithHighestBuildNumber(previousTagsForThisModule);
		} catch (final Exception e) {
			throw new MojoExecutionException(
					format("Error while detecting whether or not %s has changed since the last release",
							project.getArtifactId()),
					e);
		}
	}

	private static AnnotatedTag tagWithHighestBuildNumber(final List<AnnotatedTag> tags) {
		AnnotatedTag cur = null;
		for (final AnnotatedTag tag : tags) {
			if (cur == null || tag.buildNumber() > cur.buildNumber()) {
				cur = tag;
			}
		}
		return cur;
	}
}
