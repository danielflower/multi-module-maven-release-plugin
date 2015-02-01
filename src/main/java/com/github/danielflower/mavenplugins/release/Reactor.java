package com.github.danielflower.mavenplugins.release;

import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

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

    public static Reactor fromProjects(Git git, List<MavenProject> projects, String buildNumber) throws ValidationException, GitAPIException {
        List<ReleasableModule> modules = new ArrayList<ReleasableModule>();
        VersionNamer versionNamer = new VersionNamer(Clock.SystemClock);
        for (MavenProject project : projects) {
            ReleasableModule module = new ReleasableModule(project, buildNumber, versionNamer.name(project.getVersion(), buildNumber));
            String tagToFind = module.getNewVersion().substring(0, module.getNewVersion().lastIndexOf(".") + 1);
            Ref ref = GitHelper.refStartingWith(git, tagToFind);
            if (shouldRelease(ref)) {
                modules.add(module);
            }
        }
        return new Reactor(modules);
    }

    private static boolean shouldRelease(Ref ref) {
        return ref == null;
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
