package com.github.danielflower.mavenplugins.release;

import org.apache.maven.project.MavenProject;

public class ReleasableModule {

    private final MavenProject project;
    private final String buildNumber;
    private final String tagName;
    private final String newVersion;

    public ReleasableModule(MavenProject project, String buildNumber, VersionNamer versionNamer) throws ValidationException {
        this.project = project;
        this.buildNumber = buildNumber;
        this.newVersion = versionNamer.name(project.getVersion(), buildNumber);
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
