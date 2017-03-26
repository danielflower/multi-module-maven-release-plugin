package com.github.danielflower.mavenplugins.release;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

public class Reactor {

    private final List<ReleasableModule> modulesInBuildOrder;

    public Reactor(List<ReleasableModule> modulesInBuildOrder) {
        this.modulesInBuildOrder = modulesInBuildOrder;
    }

    public List<ReleasableModule> getModulesInBuildOrder() {
        return modulesInBuildOrder;
    }

    public static Reactor fromProjects(Log log, LocalGitRepo gitRepo, MavenProject rootProject,
                                       List<MavenProject> projects, Long buildNumber,
                                       List<String> modulesToForceRelease,
                                       NoChangesAction actionWhenNoChangesDetected, boolean bugfixRelease)
            throws ValidationException, GitAPIException, MojoExecutionException {
        DiffDetector detector = new TreeWalkingDiffDetector(gitRepo.git.getRepository());
        List<ReleasableModule> modules = new ArrayList<>();
        AnnotatedTagFinder tagFinder = new AnnotatedTagFinder(bugfixRelease);
        VersionNamer versionNamer = new VersionNamer(bugfixRelease);
        for (MavenProject project : projects) {
            String relativePathToModule = calculateModulePath(rootProject, project);
            String artifactId = project.getArtifactId();
            String versionWithoutBuildNumber = project.getVersion().replace("-SNAPSHOT", "");
            List<AnnotatedTag> previousTagsForThisModule = tagFinder.tagsForVersion(gitRepo.git, artifactId, versionWithoutBuildNumber);


            Collection<VersionInfoImpl> previousVersionInfos = new ArrayList<>();
            if (previousTagsForThisModule != null) {
                for (AnnotatedTag previousTag : previousTagsForThisModule) {
                    previousVersionInfos.add(previousTag.versionInfo());
                }
            }

            Collection<VersionInfoImpl> remoteVersionInfos = getRemoteBuildNumbers(gitRepo, artifactId,
                                                                                versionWithoutBuildNumber,
                                                                               tagFinder);
            previousVersionInfos.addAll(remoteVersionInfos);

            VersionName newVersion = versionNamer.name(project.getVersion(), buildNumber, previousVersionInfos);

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
                log.info("Releasing " + artifactId + " " + newVersion.releaseVersion() + " as we was asked to forced release.");
            }else if (oneOfTheDependenciesHasChanged) {
                log.info("Releasing " + artifactId + " " + newVersion.releaseVersion() + " as " + changedDependency + " has changed.");
            } else {
                AnnotatedTag previousTagThatIsTheSameAsHEADForThisModule = hasChangedSinceLastRelease(previousTagsForThisModule, detector, project, relativePathToModule);
                if (previousTagThatIsTheSameAsHEADForThisModule != null) {
                    equivalentVersion = previousTagThatIsTheSameAsHEADForThisModule.version() + "." + previousTagThatIsTheSameAsHEADForThisModule.versionInfo();
                    log.info("Will use version " + equivalentVersion + " for " + artifactId + " as it has not been changed since that release.");
                } else {
                    log.info("Will use version " + newVersion.releaseVersion() + " for " + artifactId + " as it has changed since the last release.");
                }
            }
            ReleasableModule module = new ReleasableModule(project, newVersion, equivalentVersion, relativePathToModule);
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
                    List<ReleasableModule> newList = new ArrayList<ReleasableModule>();
                    for (ReleasableModule module : modules) {
                        newList.add(module.createReleasableVersion());
                    }
                    modules = newList;
            }
        }

        return new Reactor(modules);
    }

    private static Collection<VersionInfoImpl> getRemoteBuildNumbers(LocalGitRepo gitRepo, String artifactId,
                                                                 String versionWithoutBuildNumber,
                                                                 AnnotatedTagFinder tagFinder) throws GitAPIException {
        Collection<Ref> remoteTagRefs = gitRepo.allRemoteTags();
        Collection<VersionInfoImpl> remoteVersionInfos = new ArrayList<>();
        String tagWithoutBuildNumber = artifactId + "-" + versionWithoutBuildNumber;
        for (Ref remoteTagRef : remoteTagRefs) {
            String remoteTagName = remoteTagRef.getName();
            VersionInfoImpl versionInfo = tagFinder.buildNumberOf(tagWithoutBuildNumber, remoteTagName);
            if (versionInfo != null) {
                remoteVersionInfos.add(versionInfo);
            }
        }
        return remoteVersionInfos;
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

    static AnnotatedTag hasChangedSinceLastRelease(List<AnnotatedTag> previousTagsForThisModule, DiffDetector detector, MavenProject project, String relativePathToModule) throws MojoExecutionException {
        try {
            if (previousTagsForThisModule.size() == 0) return null;
            boolean hasChanged = detector.hasChangedSince(relativePathToModule, project.getModel().getModules(), previousTagsForThisModule);
            return hasChanged ? null : tagWithHighestBuildNumber(previousTagsForThisModule);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while detecting whether or not " + project.getArtifactId() + " has changed since the last release", e);
        }
    }

    private static AnnotatedTag tagWithHighestBuildNumber(List<AnnotatedTag> tags) {
        return Collections.max(tags, new Comparator<AnnotatedTag>() {
            @Override
            public int compare(AnnotatedTag o1, AnnotatedTag o2) {
                return o1.versionInfo().compareTo(o2.versionInfo());
            }
        });
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
