package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.lib.Repository;

import java.util.Collection;

import static java.util.Arrays.asList;

public class VersionNamer {

    public VersionName name(String pomVersion, Long buildNumber, Collection<Long> previousBuildNumbers) throws ValidationException {

        if (buildNumber == null) {
            if (previousBuildNumbers.size() == 0) {
                buildNumber = 0L;
            } else {
                buildNumber = nextBuildNumber(previousBuildNumbers);
            }
        }

        VersionName versionName = new VersionName(pomVersion, pomVersion.replace("-SNAPSHOT", ""), buildNumber);

        if (!Repository.isValidRefName("refs/tags/" + versionName.releaseVersion())) {
            String summary = "Sorry, '" + versionName.releaseVersion() + "' is not a valid version.";
            throw new ValidationException(summary, asList(
                summary,
                "Version numbers are used in the Git tag, and so can only contain characters that are valid in git tags.",
                "Please see https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html for tag naming rules."
            ));
        }
        return versionName;
    }

    private static long nextBuildNumber(Collection<Long> previousBuildNumbers) {
        long max = 0;
        for (Long buildNumber : previousBuildNumbers) {
            max = Math.max(max, buildNumber);
        }
        return max + 1;
    }

}
