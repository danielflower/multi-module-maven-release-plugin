package com.github.danielflower.mavenplugins.release.releaseinfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.versioning.GsonFactory;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableReleaseInfo;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;
import com.google.gson.Gson;

/**
 * Loading and storing of release-info files.
 */
public class ReleaseInfoStorage {
    public static final String RELEASE_INFO_FILE = ".release-info.json";

    private final Git          git;
    private       File         baseDir;

    public ReleaseInfoStorage(File basedir, Git git) {
        this.baseDir = basedir;
        this.git = git;
    }

    public ReleaseInfo load() throws MojoExecutionException {
        final File releaseInfoFile = new File(baseDir, RELEASE_INFO_FILE);
        ReleaseInfo previousRelease;
        if (releaseInfoFile.exists()) {
            try {
                final String json = org.apache.commons.io.FileUtils
                                        .readFileToString(releaseInfoFile, StandardCharsets.UTF_8);
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

    public void store(ImmutableReleaseInfo releaseInfo) {
        final File releaseInfoFile = new File(baseDir, RELEASE_INFO_FILE);
        try {
            final Gson gson = new GsonFactory().createGson();
            FileUtils.write(releaseInfoFile, gson.toJson(releaseInfo), StandardCharsets.UTF_8);
            git.add().addFilepattern(RELEASE_INFO_FILE).call();
            git.commit().setMessage("updating release versions").call();
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("unable to store and commit release info", e);
        }
    }
}
