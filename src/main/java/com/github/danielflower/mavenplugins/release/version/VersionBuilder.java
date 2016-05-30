package com.github.danielflower.mavenplugins.release.version;

import org.apache.maven.project.MavenProject;

public interface VersionBuilder {

	VersionBuilder setProject(MavenProject project);

	VersionBuilder setUseLastDigitAsBuildNumber(boolean useLastDigitAsBuildNumber);

	VersionBuilder setBuildNumber(Long buildNumberOrNull);

	VersionBuilder setRelativePath(String relativePathToModuleOrNull);

	VersionBuilder setChangedDependency(String changedDependencyOrNull);

	VersionBuilder setRemoteUrl(String remoteUrl);

	Version build() throws VersionException;
}
