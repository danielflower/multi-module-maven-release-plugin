package com.github.danielflower.mavenplugins.release.version;

import org.apache.maven.project.MavenProject;

/**
 * Factory interface for creating new {@link Version} instances.
 */
public interface VersionFactory {

	Version newVersion(MavenProject project, Long buildNumberOrNull, String remoteUrl) throws VersionException;
}
