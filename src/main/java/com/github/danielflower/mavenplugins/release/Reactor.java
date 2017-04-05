package com.github.danielflower.mavenplugins.release;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.repository.LocalGitRepo;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableQualifiedArtifact;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;

public class Reactor {

    private final List<ReleasableModule> modulesInBuildOrder;
    private final Map<ImmutableQualifiedArtifact, ReleasableModule> artifactModuleMap = new HashMap<>();

    public Reactor(List<ReleasableModule> modulesInBuildOrder) {
        this.modulesInBuildOrder = modulesInBuildOrder;
        modulesInBuildOrder.forEach(m -> {
            final ImmutableQualifiedArtifact key = ImmutableQualifiedArtifact.builder().artifactId(
                m.getProject().getArtifactId()).groupId(m.getProject().getGroupId()).build();
            artifactModuleMap.put(key, m);
        });
    }

    public List<ReleasableModule> getModulesInBuildOrder() {
        return modulesInBuildOrder;
    }

    public static Reactor fromProjects(Log log, LocalGitRepo gitRepo, MavenProject rootProject,
                                       List<MavenProject> projects, List<String> modulesToForceRelease,
                                       NoChangesAction actionWhenNoChangesDetected, boolean bugfixRelease,
                                       ReleaseInfo previousRelease)
            throws ValidationException, GitAPIException, MojoExecutionException {
        if (previousRelease.isEmpty()) {
            log.warn("no info file for previous releases found, assuming initial release");
        } else {
            log.info("previous release: " + previousRelease.toString());
        }
        TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(gitRepo.git.getRepository());
        List<ReleasableModule> modules = new ArrayList<>();
        for (MavenProject project : projects) {
            modules.add(new ModuleDependencyVerifier(project, rootProject, gitRepo, previousRelease,
                                                     modulesToForceRelease,
                                                     modules, bugfixRelease, log).releaseInfo());
        }

        if (!atLeastOneBeingReleased(modules)) {
            switch (actionWhenNoChangesDetected) {
                case ReleaseNone:
                    log.warn("No changes have been detected in any modules so will not perform release");
                    return null;
                case FailBuild:
                    throw new MojoExecutionException("No module changes have been detected");
                default:
                    log.warn("No changes have been detected in any modules so will re-release them all");
                    modules.clear();
                    for (MavenProject project : projects) {
                        modules.add(new ModuleDependencyVerifier(project, rootProject, gitRepo, previousRelease,
                                                                 modulesToForceRelease,
                                                                 modules, bugfixRelease, log).rereleaseModule());
                    }

            }
        }

        return new Reactor(modules);
    }

    private static boolean atLeastOneBeingReleased(List<ReleasableModule> modules) {
        for (ReleasableModule module : modules) {
            if (module.isToBeReleased()) {
                return true;
            }
        }
        return false;
    }

    public ReleasableModule find(String groupId, String artifactId) throws UnresolvedSnapshotDependencyException {
        final ImmutableQualifiedArtifact artifact = ImmutableQualifiedArtifact.builder().groupId(groupId)
                                                                           .artifactId(artifactId).build();
        if (artifactModuleMap.containsKey(artifact)) {
            return artifactModuleMap.get(artifact);
        } else {
            throw new UnresolvedSnapshotDependencyException(groupId, artifactId);
        }
    }
}
