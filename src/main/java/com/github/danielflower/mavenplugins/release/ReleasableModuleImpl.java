package com.github.danielflower.mavenplugins.release;

import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.versioning.FixVersion;

public class ReleasableModuleImpl implements ReleasableModule {

    private final MavenProject project;
    private final FixVersion   version;

    @Override
    public String getEquivalentVersion() {
        return equivalentVersion;
    }

    private final String equivalentVersion;
    private final String relativePathToModule;

    public ReleasableModuleImpl(MavenProject project, FixVersion version, String equivalentVersion, String relativePathToModule) {
        this.project = project;
        this.version = version;
        this.equivalentVersion = equivalentVersion;
        this.relativePathToModule = relativePathToModule;
    }

    @Override
    public String getNewVersion() {
        return version.versionAsString();
    }

    @Override
    public String getArtifactId() {
        return project.getArtifactId();
    }

    @Override
    public String getGroupId() {
        return project.getGroupId();
    }

    @Override
    public MavenProject getProject() {
        return project;
    }

    @Override
    public long getVersion() {
        return version.getMajorVersion();
    }

    @Override
    public FixVersion versionInfo() {
        return version;
    }

    @Override
    public String getRelativePathToModule() {
        return relativePathToModule;
    }
}
