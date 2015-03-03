package com.github.danielflower.mavenplugins.release;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

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

    public static Reactor fromProjects(Log log, Git git, MavenProject rootProject, List<MavenProject> projects, String buildNumber) throws ValidationException, GitAPIException, MojoExecutionException {
        DiffDetector detector = new DiffDetector(git.getRepository());
        List<ReleasableModule> modules = new ArrayList<ReleasableModule>();
        VersionNamer versionNamer = new VersionNamer(Clock.SystemClock);
        for (MavenProject project : projects) {
            VersionName newVersion = versionNamer.name(project.getVersion(), buildNumber);

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
                }
                if (oneOfTheDependenciesHasChanged) {
                    break;
                }
            }

            String equivalentVersion = null;
            String relativePathToModule = calculateModulePath(rootProject, project);
            if (oneOfTheDependenciesHasChanged) {
                log.info("Releasing " + project.getArtifactId() + " " + newVersion + " as " + changedDependency + " has changed.");
            } else {
                AnnotatedTag previousTagThatIsTheSameAsHEADForThisModule = hasChangedSinceLastRelease(git, detector, project, relativePathToModule);
                if (previousTagThatIsTheSameAsHEADForThisModule != null) {
                    equivalentVersion = previousTagThatIsTheSameAsHEADForThisModule.version() + "." + previousTagThatIsTheSameAsHEADForThisModule.buildNumber();
                    log.info("Will use version " + equivalentVersion + " for " + project.getArtifactId() + " as it has not been changed since that release.");
                } else {
                    log.debug("Will use version " + newVersion + " for " + project.getArtifactId() + " as it has changed since the last release.");
                }
            }
            ReleasableModule module = new ReleasableModule(project, newVersion, equivalentVersion, relativePathToModule);
            modules.add(module);
        }

        if (!atLeastOneBeingReleased(modules)) {
            log.warn("No changes have been detected in any modules so will re-release them all");
            List<ReleasableModule> newList = new ArrayList<ReleasableModule>();
            for (ReleasableModule module : modules) {
                newList.add(module.createReleasableVersion());
            }
            modules = newList;
        }

        return new Reactor(modules);
    }

    private static boolean atLeastOneBeingReleased(List<ReleasableModule> modules) {
        for (ReleasableModule module : modules) {
            if (module.willBeReleased()) {
                return true;
            }
        }
        return false;
    }

    private static String calculateModulePath(MavenProject rootProject, MavenProject project) {
        String relativePathToModule = Repository.stripWorkDir(rootProject.getBasedir(), project.getBasedir());
        if (relativePathToModule.length() == 0) {
            relativePathToModule = ".";
        }
        return relativePathToModule;
    }

    private static AnnotatedTag hasChangedSinceLastRelease(Git git, DiffDetector detector, MavenProject project, String relativePathToModule) throws MojoExecutionException {
        try {
            List<AnnotatedTag> previousTagsForThisModule = AnnotatedTagFinder.mostRecent(git, project.getArtifactId(), project.getVersion().replace("-SNAPSHOT", ""));
            if (previousTagsForThisModule.size() == 0) {
                return null;
            }
            boolean hasChanged = detector.hasChangedSince(relativePathToModule, project.getModel().getModules(), previousTagsForThisModule);
            return hasChanged ? null : previousTagsForThisModule.get(0);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while detecting whether or not " + project.getArtifactId() + " has changed since the last release", e);
        }
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
