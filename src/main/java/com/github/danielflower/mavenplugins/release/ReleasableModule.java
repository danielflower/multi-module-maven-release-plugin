package com.github.danielflower.mavenplugins.release;

import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Repository;

import java.util.List;

public class ReleasableModule {

    private final MavenProject project;
    private final String version;
    private final String buildNumber;
    private final String tagName;
    private final String newVersion;

    public ReleasableModule(MavenProject project, String version, String buildNumber, String newVersion) throws ValidationException {
        this.project = project;
        this.version = version;
        this.buildNumber = buildNumber;
        this.newVersion = newVersion;
        this.tagName = project.getArtifactId() + "-" + this.newVersion;
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

    public String getVersion() {
        return version;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public boolean isOneOf(List<String> moduleNames) {
        String modulePath = project.getBasedir().getName();
        for (String moduleName : moduleNames) {
            if (modulePath.equals(moduleName)) {
                return true;
            }
        }
        return false;
    }
}
