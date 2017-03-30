package com.github.danielflower.mavenplugins.release;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import com.github.danielflower.mavenplugins.release.versioning.ImmutableFixVersion;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;
import com.github.danielflower.mavenplugins.release.versioning.VersionNamer;

public class Reactor {

    private final List<ReleasableModule> modulesInBuildOrder;
    private final ReleaseInfo previousRelease;

    public Reactor(List<ReleasableModule> modulesInBuildOrder, ReleaseInfo previousRelease) {
        this.modulesInBuildOrder = modulesInBuildOrder;
        this.previousRelease = previousRelease;
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
        DiffDetector detector = new TreeWalkingDiffDetector(gitRepo.git.getRepository());
        List<ReleasableModule> modules = new ArrayList<>();
        VersionNamer versionNamer = new VersionNamer(bugfixRelease, previousRelease);
        for (MavenProject project : projects) {
            String relativePathToModule = calculateModulePath(rootProject, project);
            String artifactId = project.getArtifactId();

            ImmutableFixVersion newVersion = ImmutableFixVersion.copyOf(versionNamer.nextVersion(project));

            boolean oneOfTheDependenciesHasChanged = false;
            String changedDependency = null;
            for (ReleasableModule module : modules) {
                if (module.willBeReleased()) {
                    for (Dependency dependency : project.getModel().getDependencies()) {
                        if (dependency.getGroupId().equals(module.getGroupId()) && dependency.getArtifactId().equals(module.getArtifactId())) {
                            oneOfTheDependenciesHasChanged = true;
                            changedDependency = dependency.getArtifactId();
                            break;
                        }
                    }
                    if (project.getParent() != null
                            && (project.getParent().getGroupId().equals(module.getGroupId()) && project.getParent().getArtifactId().equals(module.getArtifactId()))) {
                        oneOfTheDependenciesHasChanged = true;
                        changedDependency = project.getParent().getArtifactId();
                        break;
                    }
                }
                if (oneOfTheDependenciesHasChanged) {
                    break;
                }
            }

            String equivalentVersion = null;

            if(modulesToForceRelease != null && modulesToForceRelease.contains(artifactId)) {
                log.info("Releasing " + artifactId + " " + newVersion.toString() + " as we was asked to forced release.");
            }else if (oneOfTheDependenciesHasChanged) {
                log.info("Releasing " + artifactId + " " + newVersion.toString() + " as " + changedDependency + " has changed.");
            } else {
                // Hier passiert ein Wunder
                if (1 > 2) {
                    log.info("Will use version " + equivalentVersion + " for " + artifactId + " as it has not been changed since that release.");
                } else {
                    log.info("Will use version " + newVersion.toString() + " for " + artifactId + " as it has changed since the last release.");
                }
            }
            ReleasableModule module = new ReleasableModuleImpl(project, newVersion, equivalentVersion,
                                                               relativePathToModule);
            modules.add(module);
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
                    List<ReleasableModule> newList = new ArrayList<>();
                    for (ReleasableModule module : modules) {
                        newList.add(module.createReleasableVersion());
                    }
                    modules = newList;
            }
        }

        return new Reactor(modules, previousRelease);
    }

    private static boolean atLeastOneBeingReleased(List<ReleasableModule> modules) {
        for (ReleasableModule module : modules) {
            if (module.willBeReleased()) {
                return true;
            }
        }
        return false;
    }

    private static String calculateModulePath(MavenProject rootProject, MavenProject project) throws MojoExecutionException {
        // Getting canonical files because on Windows, it's possible one returns "C:\..." and the other "c:\..." which is rather amazing
        File projectRoot;
        File moduleRoot;
        try {
            projectRoot = rootProject.getBasedir().getCanonicalFile();
            moduleRoot = project.getBasedir().getCanonicalFile();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not find directory paths for maven project", e);
        }
        String relativePathToModule = Repository.stripWorkDir(projectRoot, moduleRoot);
        if (relativePathToModule.length() == 0) {
            relativePathToModule = ".";
        }
        return relativePathToModule;
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
