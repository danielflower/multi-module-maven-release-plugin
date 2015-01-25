package com.github.danielflower.mavenplugins.release;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    public List<File> updateVersion() throws IOException, ValidationException {
        List<File> changedPoms = new ArrayList<File>();
        for (ReleasableModule module : reactor.getModulesInBuildOrder()) {
            MavenProject project = module.getProject();

            log.info("Going to release " + module.getArtifactId() + " " + module.getNewVersion());

            Model originalModel = project.getOriginalModel();
            alterModel(project, module.getNewVersion());
            File pom = project.getFile();
            changedPoms.add(pom);
            Writer fileWriter = new FileWriter(pom);

            try {
                MavenXpp3Writer pomWriter = new MavenXpp3Writer();
                pomWriter.write(fileWriter, originalModel);
            } finally {
                fileWriter.close();
            }
        }
        return changedPoms;
    }

    private void alterModel(MavenProject project, String newVersion) throws ValidationException {
        Model originalModel = project.getOriginalModel();
        originalModel.setVersion(newVersion);


        MavenProject parent = project.getParent();
        if (parent != null && isSnapshot(parent.getVersion())) {
            String searchingFrom = "the parent reference in " + project.getFile().getAbsolutePath();
            ReleasableModule parentBeingReleased = reactor.find(searchingFrom, parent.getGroupId(), parent.getArtifactId());
            originalModel.getParent().setVersion(parentBeingReleased.getNewVersion());
        }
        for (Dependency dependency : originalModel.getDependencies()) {
            if (isSnapshot(dependency.getVersion())) {
                String searchingFrom = "a dependency in " + project.getFile().getAbsolutePath();
                ReleasableModule dependencyBeingReleased = reactor.find(searchingFrom, dependency.getGroupId(), dependency.getArtifactId());
                dependency.setVersion(dependencyBeingReleased.getNewVersion());
            }
        }
    }

    private boolean isSnapshot(String version) {
        return (version != null && version.endsWith("-SNAPSHOT"));
    }

}
