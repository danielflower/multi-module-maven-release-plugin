package com.github.danielflower.mavenplugins.release.reactor;

import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.version.Version;

import java.util.List;

public class ReleasableModule {

    private final MavenProject project;
    private final Version version;
    private final String tagName;
    private final String equivalentVersion;
    private final String relativePathToModule;

    public ReleasableModule(MavenProject project, Version version, String equivalentVersion, String relativePathToModule) {
        this.project = project;
        this.version = version;
        this.equivalentVersion = equivalentVersion;
        this.relativePathToModule = relativePathToModule;
        this.tagName = project.getArtifactId() + "-" + version.getReleaseVersion();
    }

    public String getTagName() {
        return tagName;
    }

    public String getNewVersion() {
        return version.getReleaseVersion();
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
        return version.getBusinessVersion();
    }

    public long getBuildNumber() {
        return version.getBuildNumber();
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

    public boolean willBeReleased() {
        return equivalentVersion == null;
    }

    public String getVersionToDependOn() {
        return willBeReleased() ? version.getReleaseVersion() : equivalentVersion;
    }

    public String getRelativePathToModule() {
        return relativePathToModule;
    }

    public ReleasableModule createReleasableVersion() {
        return new ReleasableModule(project, version, null, relativePathToModule);
    }
}
