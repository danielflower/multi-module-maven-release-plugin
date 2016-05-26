package com.github.danielflower.mavenplugins.release.version;

import static java.lang.String.format;

import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.scm.ProposedTag;
import com.github.danielflower.mavenplugins.release.scm.SCMException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

class VersionBuilder {
	private final Log log;
	private final SCMRepository repository;
	private String relativePathToModule;
	private MavenProject project;
	private long buildNumber;
	private String changedDependency;
	private String businessVersion;
	private boolean useLastDigitAsBuildNumber;

	VersionBuilder(final Log log, final SCMRepository repository) {
		this.log = log;
		this.repository = repository;
	}

	public VersionBuilder setUseLastDigitAsBuildNumber(final boolean useLastDigitAsBuildNumber) {
		this.useLastDigitAsBuildNumber = useLastDigitAsBuildNumber;
		return this;
	}

	public VersionBuilder setBuildNumber(final long buildNumber) {
		this.buildNumber = buildNumber;
		return this;
	}

	public VersionBuilder setProject(final MavenProject project) {
		this.project = project;
		return this;
	}

	public VersionBuilder setChangedDependency(final String changedDependency) {
		this.changedDependency = changedDependency;
		return this;
	}

	public VersionBuilder setBusinessVersion(final String businessVersion) {
		this.businessVersion = businessVersion;
		return this;
	}

	public VersionBuilder setRelativePathToModule(final String relativePathToModule) {
		this.relativePathToModule = relativePathToModule;
		return this;
	}

	public Version build() throws VersionException {
		final String releaseVersion = businessVersion + "." + buildNumber;

		if (relativePathToModule == null) {
			log.info(format("Releasing %s %s as we was asked to forced release.", project.getArtifactId(),
					releaseVersion));
		}

		if (changedDependency != null) {
			log.info(format("Releasing %s %s as %s has changed.", project.getArtifactId(), releaseVersion,
					changedDependency));
		}

		String equivalentVersion = null;
		if (relativePathToModule != null && changedDependency == null) {
			final ProposedTag previousTagThatIsTheSameAsHEADForThisModule = hasChangedSinceLastRelease();
			if (previousTagThatIsTheSameAsHEADForThisModule != null) {
				equivalentVersion = previousTagThatIsTheSameAsHEADForThisModule.version() + "."
						+ previousTagThatIsTheSameAsHEADForThisModule.buildNumber();
				log.info(format("Will use version %s for %s as it has not been changed since that release.",
						equivalentVersion, project.getArtifactId()));
			} else {
				log.info(format("Will use version %s for %s as it has changed since the last release.", releaseVersion,
						project.getArtifactId()));
			}
		}

		return new DefaultVersion(equivalentVersion, releaseVersion, project.getVersion(), businessVersion, buildNumber,
				useLastDigitAsBuildNumber);
	}

	private ProposedTag hasChangedSinceLastRelease() throws VersionException {
		try {
			final List<ProposedTag> previousTagsForThisModule = repository.tagsForVersion(project.getArtifactId(),
					businessVersion);
			if (previousTagsForThisModule.size() == 0) {
				return null;
			}
			final boolean hasChanged = repository.hasChangedSince(relativePathToModule, project.getModules(),
					previousTagsForThisModule);
			return hasChanged ? null : tagWithHighestBuildNumber(previousTagsForThisModule);
		} catch (final SCMException e) {
			throw new VersionException(e, "Error while detecting whether or not %s has changed since the last release",
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
}
