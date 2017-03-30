package com.github.danielflower.mavenplugins.release.releaseinfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.versioning.GsonFactory;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableReleaseInfo;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;

public class ReleaseInfoLoader {
    private File baseDir;

    public ReleaseInfoLoader(MavenProject project) {
        this.baseDir = project.getBasedir();
    }

    public ReleaseInfo invoke() throws MojoExecutionException {
        final File releaseInfoFile = new File(baseDir,   ".release-info.json");
        ReleaseInfo previousRelease;
        if (releaseInfoFile.exists()) {
            try {
                final String json = org.apache.commons.io.FileUtils.readFileToString(releaseInfoFile, StandardCharsets.UTF_8);
                previousRelease = new GsonFactory().createGson().fromJson(json, ImmutableReleaseInfo.class);
            } catch (IOException e) {
                throw new MojoExecutionException("unable to read release info file " + releaseInfoFile
                                                                                           .getAbsolutePath(), e);
            }
        } else {
            previousRelease = ImmutableReleaseInfo.builder().build();
        }
        return previousRelease;
    }
}
