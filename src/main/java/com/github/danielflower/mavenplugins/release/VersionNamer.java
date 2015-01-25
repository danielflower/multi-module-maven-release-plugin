package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.lib.Repository;

import static java.util.Arrays.asList;

public class VersionNamer {
    public String name(String pomVersion, String releaseVersion) throws ValidationException {
        String newVersion = pomVersion.replace("-SNAPSHOT", "").concat(".").concat(releaseVersion);
        if (!Repository.isValidRefName("refs/tags/" + newVersion)) {
            String summary = "Sorry, '" + newVersion + "' is not a valid version.";
            throw new ValidationException(summary, asList(
                summary,
                "Version numbers are used in the Git tag, and so can only contain characters that are valid in git tags.",
                "Please see https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html for tag naming rules."
            ));
        }
        return newVersion;
    }
}
