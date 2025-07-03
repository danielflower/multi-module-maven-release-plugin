package com.github.danielflower.mavenplugins.release;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.github.danielflower.mavenplugins.release.MavenVersionResolver.resolveVersionsDefinedThroughProperties;

public class Reactor {

    private final List<ReleasableModule> modulesInBuildOrder;

    public Reactor(List<ReleasableModule> modulesInBuildOrder) {
        this.modulesInBuildOrder = modulesInBuildOrder;
    }

    public List<ReleasableModule> getModulesInBuildOrder() {
        return modulesInBuildOrder;
    }

    public static Reactor fromProjects(BaseMojo mojo, Log log, LocalGitRepo gitRepo, NoChangesAction actionWhenNoChangesDetected, ResolverWrapper resolverWrapper) throws ValidationException, GitAPIException, MojoExecutionException {
        DiffDetector detector = new TreeWalkingDiffDetector(gitRepo.git.getRepository(), mojo.getIgnoredPaths(), mojo.getRequiredPaths());
        List<ReleasableModule> modules = new ArrayList<ReleasableModule>();

        resolveVersionsDefinedThroughProperties(mojo.getProjects());

        AnnotatedTagFinder annotatedTagFinder = new AnnotatedTagFinder(mojo.getVersionNamer());
        List<String> moduleReleaseLogMessages = new ArrayList<>();

        for (MavenProject project : mojo.getProjects()) {
            String relativePathToModule = calculateModulePath(mojo.getProject(), project);
            String artifactId = project.getArtifactId();
            String versionWithoutBuildNumber = project.getVersion().replace("-SNAPSHOT", "");
            List<AnnotatedTag> previousTagsForThisModule = annotatedTagFinder.tagsForVersion(gitRepo.git, project.getGroupId(), artifactId, versionWithoutBuildNumber, mojo.getTagNameFormat(), log);


            Collection<Long> previousBuildNumbers = new ArrayList<Long>();
            if (previousTagsForThisModule != null) {
                for (AnnotatedTag previousTag : previousTagsForThisModule) {
                    previousBuildNumbers.add(previousTag.buildNumber());
                }
            }

            Collection<Long> remoteBuildNumbers = getRemoteBuildNumbers(gitRepo, project.getGroupId(), artifactId, versionWithoutBuildNumber, mojo.getVersionNamer(), mojo.getTagNameFormat(), log);
            previousBuildNumbers.addAll(remoteBuildNumbers);

            VersionName newVersion = mojo.getVersionNamer().name(project.getVersion(), mojo.getBuildNumber(), previousBuildNumbers);

            boolean oneOfTheDependenciesHasChanged = false;
            String changedDependency = null;
            for (ReleasableModule module : modules) {
                if (module.willBeReleased()) {
                    for (Dependency dependency : project.getModel().getDependencies()) {
                        if (hasSameMavenGA(module, dependency)) {
                            oneOfTheDependenciesHasChanged = true;
                            changedDependency = dependency.getArtifactId();
                            break;
                        }
                    }

                    if (project.getModel().getDependencyManagement() != null) {
                        for (Dependency dependency : project.getModel().getDependencyManagement().getDependencies()) {
                            if (hasSameMavenGAByDependencyLocation(module, dependency)) {
                                oneOfTheDependenciesHasChanged = true;
                                changedDependency = module.getArtifactId();
                                break;
                            }
                        }
                    }

                    if (project.getParent() != null && hasSameMavenGA(module, project.getParent())) {
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

            if (mojo.getModulesToForceRelease() != null && mojo.getModulesToForceRelease().contains(artifactId)) {
                moduleReleaseLogMessages.add("Releasing " + artifactId + " " + newVersion.releaseVersion() + " as we were asked to force release.");
            } else if (oneOfTheDependenciesHasChanged) {
                moduleReleaseLogMessages.add("Releasing " + artifactId + " " + newVersion.releaseVersion() + " as " + changedDependency + " has changed.");
            } else {
                AnnotatedTag previousTagThatIsTheSameAsHEADForThisModule = hasChangedSinceLastRelease(previousTagsForThisModule, detector, project, relativePathToModule);
                if (previousTagThatIsTheSameAsHEADForThisModule != null) {
                    equivalentVersion = previousTagThatIsTheSameAsHEADForThisModule.version() + mojo.getVersionNamer().getDelimiter() + previousTagThatIsTheSameAsHEADForThisModule.buildNumber();
                    // attempt to resolve
                    if (resolverWrapper.isResolvable(project.getGroupId(), project.getArtifactId(), equivalentVersion, project.getPackaging(), log)) {
                        moduleReleaseLogMessages.add("Will use version " + equivalentVersion + " for " + artifactId + " as it has not been changed since that release.");
                    } else {
                        moduleReleaseLogMessages.add("Will use version " + newVersion.releaseVersion() + " for " + artifactId + " as although no change was detected, the artifact cannot be resolved!");
                        equivalentVersion = null;
                    }
                } else {
                    moduleReleaseLogMessages.add("Will use version " + newVersion.releaseVersion() + " for " + artifactId + " as it has changed since the last release.");
                }
            }
            ReleasableModule module = new ReleasableModule(project, newVersion, equivalentVersion, relativePathToModule, mojo.getTagNameFormat(), log);
            modules.add(module);
        }

        if (atLeastOneBeingReleased(modules)) {
            for (String moduleReleaseLogMessage : moduleReleaseLogMessages) {
                log.info(moduleReleaseLogMessage);
            }
        }
        else {
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
                        log.info("Will use version " + module.getNewVersion() + " for " + module.getArtifactId() + " as it has not been changed since that release.");
                        newList.add(module.createReleasableVersion(log));
                    }
                    modules = newList;
            }
        }

        return new Reactor(modules);
    }

    private static boolean hasSameMavenGA(ReleasableModule module, Dependency dependency) {
        return dependency.getGroupId().equals(module.getGroupId()) && dependency.getArtifactId().equals(module.getArtifactId());
    }

    private static boolean hasSameMavenGA(ReleasableModule module, MavenProject project) {
        return project.getGroupId().equals(module.getGroupId()) && project.getArtifactId().equals(module.getArtifactId());
    }

    private static boolean hasSameMavenGAByDependencyLocation(ReleasableModule module, Dependency dependency) {
        InputLocation depLocation = dependency.getLocation("");
        String[] modelId;
        if (depLocation == null) {
            modelId = new String[2];
            modelId[0] = dependency.getArtifactId();
            modelId[1] = dependency.getGroupId();
        } else {
            modelId = depLocation.getSource().getModelId().split(":");
        }
        return modelId[0].equals(module.getGroupId()) && modelId[1].equals(module.getArtifactId());
    }

    private static Collection<Long> getRemoteBuildNumbers(LocalGitRepo gitRepo, String groupId, String artifactId, String versionWithoutBuildNumber, VersionNamer versionNamer, String tagNameFormat, Log log) throws GitAPIException {
        Collection<Ref> remoteTagRefs = gitRepo.allTags();
        Collection<Long> remoteBuildNumbers = new ArrayList<Long>();
        String tagWithoutBuildNumber = artifactId + "-" + versionWithoutBuildNumber;
        if (StringUtils.isNotEmpty(tagNameFormat)) {
            tagWithoutBuildNumber = AnnotatedTag.formatTagName(tagWithoutBuildNumber, groupId, artifactId, versionWithoutBuildNumber, tagNameFormat, log);
        }

        AnnotatedTagFinder annotatedTagFinder = new AnnotatedTagFinder(versionNamer);
        for (Ref remoteTagRef : remoteTagRefs) {
            String remoteTagName = remoteTagRef.getName();
            Long buildNumber = annotatedTagFinder.buildNumberOf(tagWithoutBuildNumber, remoteTagName);
            if (buildNumber != null) {
                remoteBuildNumbers.add(buildNumber);
            }
        }
        return remoteBuildNumbers;
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
            if (previousTagsForThisModule.size() == 0) {
                return null;
            }
            boolean hasChanged = detector.hasChangedSince(relativePathToModule, project.getModel().getModules(), previousTagsForThisModule);
            return hasChanged ? null : tagWithHighestBuildNumber(previousTagsForThisModule);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while detecting whether or not " + project.getArtifactId() + " has changed since the last release", e);
        }
    }

    private static AnnotatedTag tagWithHighestBuildNumber(List<AnnotatedTag> tags) {
        AnnotatedTag cur = null;
        for (AnnotatedTag tag : tags) {
            if (cur == null || tag.buildNumber() > cur.buildNumber()) {
                cur = tag;
            }
        }
        return cur;
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
