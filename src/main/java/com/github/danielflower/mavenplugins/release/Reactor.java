package com.github.danielflower.mavenplugins.release;

import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;

public class Reactor {

    private final List<ReleasableModule> modulesInBuildOrder;

    public Reactor(List<ReleasableModule> modulesInBuildOrder) {
        this.modulesInBuildOrder = modulesInBuildOrder;
    }

    public List<ReleasableModule> getModulesInBuildOrder() {
        return modulesInBuildOrder;
    }

    public static Reactor fromProjects(List<MavenProject> projects, String buildNumber) throws ValidationException {
        List<ReleasableModule> modules = new ArrayList<ReleasableModule>();
        VersionNamer versionNamer = new VersionNamer(Clock.SystemClock);
        for (MavenProject project : projects) {
            ReleasableModule module = new ReleasableModule(project, buildNumber, versionNamer.name(project.getVersion(), buildNumber));
            modules.add(module);
        }
        return new Reactor(modules);
    }

    public ReleasableModule findByLabel(String label) {
        for (ReleasableModule module : modulesInBuildOrder) {
            String currentLabel = module.getGroupId() + ":" + module.getArtifactId();
            if (currentLabel.equals(label)) {
                return module;
            }
        }
        return null;
    }

    public ReleasableModule find(String groupId, String artifactId, String version) throws UnresolvedSnapshotDependencyException {
        ReleasableModule value = findByLabel(groupId + ":" + artifactId);
        if (value == null) {
            throw new UnresolvedSnapshotDependencyException(groupId, artifactId, version);
        }
        return value;
    }
}
