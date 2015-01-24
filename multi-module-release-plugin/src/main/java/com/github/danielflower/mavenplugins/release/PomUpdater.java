package com.github.danielflower.mavenplugins.release;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
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
    private final List<MavenProject> projects;
    private final String newVersion;

    public PomUpdater(Log log, List<MavenProject> projects, String newVersion) {
        this.log = log;
        this.projects = projects;
        this.newVersion = newVersion;
    }

    public List<File> updateVersion() throws IOException {
        List<File> changedPoms = new ArrayList<File>();
        for (MavenProject project : projects) {

            log.info("Going to release " + project.getArtifactId() + " " + newVersion);

            Model originalModel = project.getOriginalModel();
            alterModel(originalModel);
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

    private void alterModel(Model originalModel) {
        originalModel.setVersion(newVersion);
        Parent parent = originalModel.getParent();
        if (parent != null && isSnapshot(parent.getVersion())) {
            parent.setVersion(newVersion);
        }
        for (Dependency dependency : originalModel.getDependencies()) {
            if (isSnapshot(dependency.getVersion())) {
                dependency.setVersion(newVersion);
            }
        }
    }

    private boolean isSnapshot(String version) {
        return (version != null && version.endsWith("-SNAPSHOT"));
    }

}
