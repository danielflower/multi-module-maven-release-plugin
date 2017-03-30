package com.github.danielflower.mavenplugins.release;

import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.versioning.ImmutableFixVersion;

public class ReleasableModuleImpl implements ReleasableModule {

    private final MavenProject project;
    private final ImmutableFixVersion   version;

    @Override
    public String getEquivalentVersion() {
        return equivalentVersion;
    }

    private final String equivalentVersion;
    private final String relativePathToModule;

    public ReleasableModuleImpl(MavenProject project, ImmutableFixVersion version, String equivalentVersion, String relativePathToModule) {
        this.project = project;
        this.version = version;
        this.equivalentVersion = equivalentVersion;
        this.relativePathToModule = relativePathToModule;
    }

    @Override
    public String getNewVersion() {
        return version.toString();
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
    public Long getVersion() {
        return version.getMajorVersion();
    }

    @Override
    public ImmutableFixVersion versionInfo() {
        return version;
    }

    @Override
    public String getRelativePathToModule() {
        return relativePathToModule;
    }
}
