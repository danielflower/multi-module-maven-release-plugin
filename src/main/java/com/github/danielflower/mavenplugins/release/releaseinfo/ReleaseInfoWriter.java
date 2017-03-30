package com.github.danielflower.mavenplugins.release.releaseinfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.versioning.GsonFactory;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableReleaseInfo;
import com.google.gson.Gson;

public class ReleaseInfoWriter {
    private final ImmutableReleaseInfo build;
    private final File                 baseDir;

    public ReleaseInfoWriter(MavenProject project, ImmutableReleaseInfo build) {
        this.baseDir = project.getBasedir();
        this.build = build;
    }

    public void invoke() {
        final File releaseInfoFile = new File(baseDir, ".release-info.json");
        try {
            final Gson gson = new GsonFactory().createGson();
            FileUtils.write(releaseInfoFile, gson.toJson(build), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("unable to write release info file", e);
        }
    }
}
