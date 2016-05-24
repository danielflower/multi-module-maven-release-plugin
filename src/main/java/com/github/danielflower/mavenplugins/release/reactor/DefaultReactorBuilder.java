package com.github.danielflower.mavenplugins.release.reactor;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Repository;

import com.github.danielflower.mavenplugins.release.scm.ProposedTag;
import com.github.danielflower.mavenplugins.release.scm.SCMException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;
import com.github.danielflower.mavenplugins.release.version.Version;
import com.github.danielflower.mavenplugins.release.version.VersionException;
import com.github.danielflower.mavenplugins.release.version.VersionFactory;

final class DefaultReactorBuilder implements ReactorBuilder {
	private final Log log;
	private final SCMRepository repository;
	private final VersionFactory versionFactory;
	private MavenProject rootProject;
	private List<MavenProject> projects;
	private boolean useLastDigitAsBuildNumber;
	private Long buildNumber;
	private List<String> modulesToForceRelease;
	private String remoteUrl;

	public DefaultReactorBuilder(final Log log, final SCMRepository repository,
			final VersionFactory versioningFactory) {
		this.log = log;
		this.repository = repository;
		this.versionFactory = versioningFactory;
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
				final Version version = versionFactory.newVersion(project, useLastDigitAsBuildNumber, buildNumber,
						remoteUrl);
				final String relativePathToModule = calculateModulePath(rootProject, project);
				final String equivalentVersion = logReleaseInfo(project, reactor, version, relativePathToModule);

				reactor.addReleasableModule(
						new ReleasableModule(project, version, equivalentVersion, relativePathToModule));
			} catch (final VersionException e) {
				throw new ReactorException(e, "Version could be created for project %s", project);
			}
		}

		return reactor.finalizeReleaseVersions();
	}

	public ProposedTag hasChangedSinceLastRelease(final MavenProject project, final String businessVersion,
			final String relativePathToModule) throws ReactorException {
		try {
			final List<ProposedTag> previousTagsForThisModule = repository.tagsForVersion(project.getArtifactId(),
					businessVersion);
			if (previousTagsForThisModule.size() == 0) {
				return null;
			}
			final boolean hasChanged = repository.hasChangedSince(relativePathToModule, project.getModel().getModules(),
					previousTagsForThisModule);
			return hasChanged ? null : tagWithHighestBuildNumber(previousTagsForThisModule);
		} catch (final SCMException e) {
			throw new ReactorException(e, "Error while detecting whether or not %s has changed since the last release",
					project.getArtifactId());
		}
	}

	private ProposedTag tagWithHighestBuildNumber(final List<ProposedTag> previousTagsForThisModule) {
		ProposedTag cur = null;
		for (final ProposedTag tag : previousTagsForThisModule) {
			if (cur == null || tag.buildNumber() > cur.buildNumber()) {
				cur = tag;
			}
		}
		return cur;
	}

	private String logReleaseInfo(final MavenProject project, final DefaultReactor reactor, final Version versioning,
			final String relativePathToModule) throws ReactorException {
		final String changedDependencyOrNull = reactor.getChangedDependencyOrNull(project);
		String equivalentVersion = null;
		if (modulesToForceRelease != null && modulesToForceRelease.contains(project.getArtifactId())) {
			log.info(format("Releasing %s %s as we was asked to forced release.", project.getArtifactId(),
					versioning.getReleaseVersion()));
		} else if (changedDependencyOrNull != null) {
			log.info(format("Releasing %s %s as %s has changed.", project.getArtifactId(),
					versioning.getReleaseVersion(), changedDependencyOrNull));
		} else {
			final ProposedTag previousTagThatIsTheSameAsHEADForThisModule = hasChangedSinceLastRelease(project,
					versioning.getBusinessVersion(), relativePathToModule);

			if (previousTagThatIsTheSameAsHEADForThisModule != null) {
				equivalentVersion = previousTagThatIsTheSameAsHEADForThisModule.version() + "."
						+ previousTagThatIsTheSameAsHEADForThisModule.buildNumber();
				log.info(format("Will use version %s for %s as it has not been changed since that release.",
						equivalentVersion, project.getArtifactId()));
			} else {
				log.info(format("Will use version %s for %s as it has changed since the last release.",
						versioning.getReleaseVersion(), project.getArtifactId()));
			}
		}
		return equivalentVersion;
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
