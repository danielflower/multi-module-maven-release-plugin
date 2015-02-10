package com.github.danielflower.mavenplugins.release;

import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

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

    public ReleasableModule find(String groupId, String artifactId, String version) throws UnresolvedSnapshotDependencyException {

        for (ReleasableModule module : modulesInBuildOrder) {
            if (module.getGroupId().equals(groupId) && module.getArtifactId().equals(artifactId)) {
                return module;
            }
        }

        throw new UnresolvedSnapshotDependencyException(groupId, artifactId, version);
    }

}
