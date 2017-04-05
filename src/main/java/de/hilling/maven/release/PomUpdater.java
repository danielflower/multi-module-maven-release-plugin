package de.hilling.maven.release;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.WriterFactory;

import de.hilling.maven.release.versioning.ImmutableFixVersion;

public class PomUpdater {

    private final Log     log;
    private final Reactor reactor;

    public PomUpdater(Log log, Reactor reactor) {
        this.log = log;
        this.reactor = reactor;
    }

    private static boolean isMultiModuleReleasePlugin(Plugin plugin) {
        return plugin.getGroupId().equals("de.hilling.maven.release") && plugin.getArtifactId().equals(
            "smart-release-plugin");
    }

    public UpdateResult updateVersion() {
        List<File> changedPoms = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (ReleasableModule module : reactor.getModulesInBuildOrder()) {
            try {
                MavenProject project = module.getProject();
                final ImmutableFixVersion version = module.getImmutableModule().getVersion();
                if (module.isToBeReleased()) {
                    log.info("Going to release " + module.getProject().getArtifactId() + " " + version.toString());
                }

                List<String> errorsForCurrentPom = alterModel(project, version.toString());
                errors.addAll(errorsForCurrentPom);

                File pom = project.getFile().getCanonicalFile();
                changedPoms.add(pom);
                Writer fileWriter = WriterFactory.newXmlWriter(pom);

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

    private List<String> alterModel(MavenProject project, String newVersion) {
        Model originalModel = project.getOriginalModel();
        originalModel.setVersion(newVersion);

        List<String> errors = new ArrayList<String>();

        String searchingFrom = project.getArtifactId();
        MavenProject parent = project.getParent();
        if (parent != null && isSnapshot(parent.getVersion())) {
            try {
                ReleasableModule parentBeingReleased = reactor.find(parent.getGroupId(), parent.getArtifactId());
                final ImmutableFixVersion version = parentBeingReleased.getImmutableModule().getVersion();
                originalModel.getParent().setVersion(version.toString());
                log.debug(
                    " Parent " + parentBeingReleased.getProject().getArtifactId() + " rewritten to version " + version
                                                                                                                   .toString());
            } catch (UnresolvedSnapshotDependencyException e) {
                errors.add("The parent of " + searchingFrom + " is " + e.artifactId);
            }
        }

        Properties projectProperties = project.getProperties();
        for (Dependency dependency : originalModel.getDependencies()) {
            String version = dependency.getVersion();
            if (isSnapshot(resolveVersion(version, projectProperties))) {
                try {
                    ReleasableModule dependencyBeingReleased = reactor.find(dependency.getGroupId(),
                                                                            dependency.getArtifactId());
                    final ImmutableFixVersion dependencyVersion = dependencyBeingReleased.getImmutableModule()
                                                                                         .getVersion();
                    dependency.setVersion(dependencyVersion.toString());
                    log.debug(" Dependency on " + dependencyBeingReleased.getProject()
                                                                         .getArtifactId() + " rewritten to version " + dependencyVersion
                                                                                                                           .toString());
                } catch (UnresolvedSnapshotDependencyException e) {
                    errors.add(searchingFrom + " references dependency " + e.artifactId);
                }
            } else {
                log.debug(
                    " Dependency on " + dependency.getArtifactId() + " kept at version " + dependency.getVersion());
            }
        }
        for (Plugin plugin : project.getModel().getBuild().getPlugins()) {
            String version = plugin.getVersion();
            if (isSnapshot(resolveVersion(version, projectProperties))) {
                if (!isMultiModuleReleasePlugin(plugin)) {
                    errors.add(searchingFrom + " references plugin " + plugin.getArtifactId() + " " + version);
                }
            }
        }
        return errors;
    }

    private String resolveVersion(String version, Properties projectProperties) {
        if (version != null && version.startsWith("${")) {
            return projectProperties.getProperty(version.replace("${", "").replace("}", ""), version);
        }
        return version;
    }

    private boolean isSnapshot(String version) {
        return (version != null && version.endsWith("-SNAPSHOT"));
    }

    public static class UpdateResult {
        public final List<File>   alteredPoms;
        public final List<String> dependencyErrors;
        public final Exception    unexpectedException;

        public UpdateResult(List<File> alteredPoms, List<String> dependencyErrors, Exception unexpectedException) {
            this.alteredPoms = alteredPoms;
            this.dependencyErrors = dependencyErrors;
            this.unexpectedException = unexpectedException;
        }

        public boolean success() {
            return (dependencyErrors.size() == 0) && (unexpectedException == null);
        }
    }
}
