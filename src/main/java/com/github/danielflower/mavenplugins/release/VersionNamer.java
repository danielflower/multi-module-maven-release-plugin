package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.lib.Repository;

public class VersionNamer {
    public String name(String pomVersion, String releaseVersion) throws MojoExecutionException {
        String newVersion = pomVersion.replace("-SNAPSHOT", "").concat(".").concat(releaseVersion);
        if (!Repository.isValidRefName("refs/tags/" + newVersion)) {
            throw new MojoExecutionException("Sorry, '" + newVersion +
                "' is not a valid version. Please see https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html for tag naming rules.");
        }
        return newVersion;
    }
}
