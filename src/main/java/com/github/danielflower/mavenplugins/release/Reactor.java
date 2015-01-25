package com.github.danielflower.mavenplugins.release;

import org.apache.maven.project.MavenProject;

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

    public static Reactor fromProjects(List<MavenProject> projects, String releaseVersion) throws ValidationException {
        List<ReleasableModule> modules = new ArrayList<ReleasableModule>();
        for (MavenProject project : projects) {
            modules.add(new ReleasableModule(project, releaseVersion));
        }
        return new Reactor(modules);
    }


    public ReleasableModule find(String searchingFrom, String groupId, String artifactId) throws ValidationException {

        for (ReleasableModule module : modulesInBuildOrder) {
            if (module.getGroupId().equals(groupId) && module.getArtifactId().equals(artifactId)) {
                return module;
            }
        }

        String summary = "The artifact " + groupId + ":" + artifactId + " referenced from " + searchingFrom +
            " is a SNAPSHOT in your project however it was not found";
        throw new ValidationException(summary, asList(summary));
    }
}
