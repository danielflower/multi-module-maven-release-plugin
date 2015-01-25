package com.github.danielflower.mavenplugins.release;

import org.apache.maven.project.MavenProject;

public class ReleasableModule {

    private final MavenProject project;
    private final String releaseVersion;
    private final String tagName;
    private final String newVersion;

    public ReleasableModule(MavenProject project, String releaseVersion) throws ValidationException {
        this.project = project;
        this.releaseVersion = releaseVersion;
        this.newVersion = new VersionNamer().name(project.getVersion(), releaseVersion);
        this.tagName = project.getArtifactId() + "-" + newVersion;
    }

    public String getTagName() {
        return tagName;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public String getArtifactId() {
        return project.getArtifactId();
    }

    public String getGroupId() {
        return project.getGroupId();
    }

    public MavenProject getProject() {
        return project;
    }
}
