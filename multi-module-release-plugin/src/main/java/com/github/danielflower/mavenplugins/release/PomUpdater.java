package com.github.danielflower.mavenplugins.release;

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

            project.getOriginalModel().setVersion(newVersion);
            File pom = project.getFile();
            changedPoms.add(pom);
            Writer fileWriter = new FileWriter(pom);

            try {
                MavenXpp3Writer pomWriter = new MavenXpp3Writer();
                pomWriter.write(fileWriter, project.getOriginalModel());
            } finally {
                fileWriter.close();
            }
        }
        return changedPoms;
    }

}
