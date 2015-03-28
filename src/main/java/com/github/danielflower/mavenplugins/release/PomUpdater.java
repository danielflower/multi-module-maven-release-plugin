package com.github.danielflower.mavenplugins.release;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class PomUpdater {

    private final Log log;
    private final Reactor reactor;

    public PomUpdater(Log log, Reactor reactor) {
        this.log = log;
        this.reactor = reactor;
    }

    public UpdateResult updateVersion() {
        List<File> changedPoms = new ArrayList<File>();
        List<String> errors = new ArrayList<String>();
        for (ReleasableModule module : reactor.getModulesInBuildOrder()) {
            try {
                MavenProject project = module.getProject();
                if (module.willBeReleased()) {
                    log.info("Going to release " + module.getArtifactId() + " " + module.getNewVersion());
                }

                List<String> errorsForCurrentPom = alterModel(project, module.getNewVersion());
                errors.addAll(errorsForCurrentPom);

                File pom = project.getFile().getCanonicalFile();
                changedPoms.add(pom);
                Writer fileWriter = new FileWriter(pom);

                Model originalModel = project.getOriginalModel();
                try {
                    MavenXpp3Writer pomWriter = new MavenXpp3Writer();
                    pomWriter.write(fileWriter, originalModel);
                } finally {
                    fileWriter.close();
                }
            } catch (Exception e) {
                return new UpdateResult(changedPoms, errors, e);
            }
        }
        return new UpdateResult(changedPoms, errors, null);
    }

    public static class UpdateResult {
        public final List<File> alteredPoms;
        public final List<String> dependencyErrors;
        public final Exception unexpectedException;

        public UpdateResult(List<File> alteredPoms, List<String> dependencyErrors, Exception unexpectedException) {
            this.alteredPoms = alteredPoms;
            this.dependencyErrors = dependencyErrors;
            this.unexpectedException = unexpectedException;
        }
        public boolean success() {
            return (dependencyErrors.size() == 0) && (unexpectedException == null);
        }
    }

    private List<String> alterModel(MavenProject project, String newVersion) {
        Model originalModel = project.getOriginalModel();
        originalModel.setVersion(newVersion);

        List<String> errors = new ArrayList<String>();

        String searchingFrom = project.getArtifactId();
        MavenProject parent = project.getParent();
        if (parent != null && isSnapshot(parent.getVersion())) {
            try {
                ReleasableModule parentBeingReleased = reactor.find(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
                originalModel.getParent().setVersion(parentBeingReleased.getVersionToDependOn());
            } catch (UnresolvedSnapshotDependencyException e) {
                errors.add("The parent of " + searchingFrom + " is " + e.artifactId + " " + e.version);
            }
        }
        for (Dependency dependency : originalModel.getDependencies()) {
            String version = dependency.getVersion();
            if (isSnapshot(version)) {
                try {
                    ReleasableModule dependencyBeingReleased = reactor.find(dependency.getGroupId(), dependency.getArtifactId(), version);
                    dependency.setVersion(dependencyBeingReleased.getVersionToDependOn());
                } catch (UnresolvedSnapshotDependencyException e) {
                    errors.add(searchingFrom + " references dependency " + e.artifactId + " " + e.version);
                }
            }
        }
        for (Plugin plugin : project.getModel().getBuild().getPlugins()) {
            String version = plugin.getVersion();
            if (isSnapshot(version)) {
                if (!isMultiModuleReleasePlugin(plugin)) {
                    errors.add(searchingFrom + " references plugin " + plugin.getArtifactId() + " " + version);
                }
            }
        }
        return errors;
    }

    private static boolean isMultiModuleReleasePlugin(Plugin plugin) {
        return plugin.getGroupId().equals("com.github.danielflower.mavenplugins") && plugin.getArtifactId().equals("multi-module-maven-release-plugin");
    }

    private boolean isSnapshot(String version) {
        return (version != null && version.endsWith("-SNAPSHOT"));
    }

}
