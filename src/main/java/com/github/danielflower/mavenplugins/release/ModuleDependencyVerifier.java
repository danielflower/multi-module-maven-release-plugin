package com.github.danielflower.mavenplugins.release;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import com.github.danielflower.mavenplugins.release.repository.LocalGitRepo;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableFixVersion;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableModuleVersion;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableQualifiedArtifact;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseDateSingleton;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;
import com.github.danielflower.mavenplugins.release.versioning.VersionNamer;

class ModuleDependencyVerifier {
    private final ReleaseInfo            previousRelease;
    private final LocalGitRepo           gitRepo;
    private final Log                    log;
    private final MavenProject           rootProject;
    private final List<String>           modulesToForceRelease;
    private final List<ReleasableModule> modules;
    private final VersionNamer           versionNamer;
    private final MavenProject           project;

    public ModuleDependencyVerifier(MavenProject project, MavenProject rootProject, LocalGitRepo gitRepo,
                                    ReleaseInfo previousRelease, List<String> modulesToForceRelease,
                                    List<ReleasableModule> modules, boolean bugfixRelease, Log log) {
        this.gitRepo = gitRepo;
        this.log = log;
        this.rootProject = rootProject;
        this.modulesToForceRelease = modulesToForceRelease;
        this.modules = modules;
        this.previousRelease = previousRelease;
        this.versionNamer = new VersionNamer(bugfixRelease, previousRelease);
        this.project = project;
    }

    private static String calculateModulePath(MavenProject rootProject, MavenProject project) throws
                                                                                              MojoExecutionException {
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

    public ImmutableReleasableModule releaseInfo() throws MojoExecutionException {
        String relativePathToModule = calculateModulePath(rootProject, project);
        String artifactId = project.getArtifactId();

        ImmutableFixVersion newVersion = ImmutableFixVersion.copyOf(versionNamer.nextVersion(project));

        boolean dependencyChanged = modules.stream().filter(ReleasableModule::isToBeReleased)
                                           .anyMatch(this::dependencyOrParentChanged);
        ImmutableFixVersion equivalentVersion;
        boolean toBeReleased;

        final Optional<ImmutableModuleVersion> previousVersion = previousRelease.getModules().stream().filter(
            m -> m.getArtifact().equals(artifact())).findAny();
        if (modulesToForceRelease.contains(artifactId)) {
            toBeReleased = true;
            equivalentVersion = newVersion;
            log.info("Releasing " + artifactId + " " + newVersion.toString() + " as we was asked to forced release.");
        } else if (dependencyChanged) {
            toBeReleased = true;
            equivalentVersion = newVersion;
            log.info(
                "Releasing " + artifactId + " " + newVersion.toString() + " as at least one dependency has changed.");
        } else {
            final String tagName = previousVersion.map(ImmutableModuleVersion::getReleaseTag).orElse("");
            try {
                log.info("looking for tag with name '" + tagName + "'");
                final Optional<Ref> tagRef = gitRepo.getRemoteTag(tagName);
                if (tagRef.isPresent()) {
                    final Ref gitTag = tagRef.get();
                    final TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(gitRepo.git.getRepository());
                    if (detector.hasChangedSince(relativePathToModule, moduleList(), gitTag)) {
                        toBeReleased = true;
                        equivalentVersion = newVersion;
                        log.info(
                            "using " + equivalentVersion + " for " + artifactId + " as it has changed since the last " + "release.");
                    } else {
                        toBeReleased = false;
                        equivalentVersion = previousVersion.get().getVersion();
                        log.info(
                            "using " + equivalentVersion + " for " + artifactId + " as it has not been changed" + " since that release.");
                    }
                } else {
                    toBeReleased = true;
                    equivalentVersion = newVersion;
                    log.info("using " + equivalentVersion + " for " + artifactId + " as it has not been released yet.");
                }
            } catch (GitAPIException | IOException e) {
                log.error("unable to list tags: " + e.getMessage());
                throw new MojoExecutionException("unable to list tags", e);
            }
        }
        final ImmutableReleasableModule.Builder builder = ImmutableReleasableModule.builder();
        builder.project(project);
        builder.isToBeReleased(toBeReleased);
        builder.relativePathToModule(relativePathToModule);
        builder.immutableModule(moduleVersion(equivalentVersion, previousVersion, toBeReleased).build());
        return builder.build();
    }

    private ImmutableModuleVersion.Builder moduleVersion(ImmutableFixVersion equivalentVersion,
                                                         Optional<ImmutableModuleVersion> previousVersion,
                                                         boolean toBeReleased) {
        final ImmutableModuleVersion.Builder moduleBuilder = ImmutableModuleVersion.builder();
        if (previousVersion.isPresent()) {
            moduleBuilder.from(previousVersion.get());
        } else {
            final ImmutableQualifiedArtifact.Builder artifactBuilder = ImmutableQualifiedArtifact.builder();
            artifactBuilder.groupId(project.getGroupId()).artifactId(project.getArtifactId());
            moduleBuilder.artifact(artifactBuilder.build());
        }
        if (toBeReleased) {
            moduleBuilder.releaseTag(ReleaseDateSingleton.getInstance().tagName());
            moduleBuilder.releaseDate(ReleaseDateSingleton.getInstance().releaseDate());
        }
        moduleBuilder.version(equivalentVersion);
        return moduleBuilder;
    }

    public ReleasableModule rereleaseModule() throws MojoExecutionException {
        String relativePathToModule = calculateModulePath(rootProject, project);
        String artifactId = project.getArtifactId();

        ImmutableFixVersion newVersion = ImmutableFixVersion.copyOf(versionNamer.nextVersion(project));

        log.info("using " + newVersion + " for " + artifactId + " for rerelease.");
        final ImmutableReleasableModule.Builder builder = ImmutableReleasableModule.builder();
        builder.project(project);
        builder.immutableModule(moduleVersion(newVersion, Optional.empty(), true).build());
        builder.isToBeReleased(true);
        builder.relativePathToModule(relativePathToModule);
        return builder.build();
    }

    private List<String> moduleList() {
        return project.getModules();
    }

    private ImmutableQualifiedArtifact artifact() {
        return ImmutableQualifiedArtifact.builder().groupId(project.getGroupId()).artifactId(project.getArtifactId())
                                         .build();
    }

    private boolean dependencyOrParentChanged(ReleasableModule module) {
        for (Dependency dependency : project.getModel().getDependencies()) {
            if (moduleIsADependency(module, dependency)) {
                return true;
            }
        }
        return isThisProjectsParentModule(module);
    }

    private boolean moduleIsADependency(ReleasableModule module, Dependency dependency) {
        return dependency.getGroupId().equals(module.getProject().getGroupId()) && dependency.getArtifactId().equals(
            module.getProject().getArtifactId());
    }

    private boolean isThisProjectsParentModule(ReleasableModule module) {
        final MavenProject parent = project.getParent();
        return parent != null && (parent.getGroupId().equals(module.getProject().getGroupId()) && parent.getArtifactId()
                                                                                                        .equals(module
                                                                                                                    .getProject()
                                                                                                                    .getArtifactId()));
    }
}
