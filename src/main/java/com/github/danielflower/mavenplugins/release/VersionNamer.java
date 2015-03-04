package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.lib.Repository;

import java.util.List;

import static java.util.Arrays.asList;

public class VersionNamer {

    public VersionName name(String pomVersion, String buildNumber, List<AnnotatedTag> previousTags) throws ValidationException {
        if (buildNumber == null || buildNumber.trim().length() == 0) {
            if (previousTags == null || previousTags.size() == 0) {
                buildNumber = "0";
            } else {
                buildNumber = String.valueOf(nextBuildNumber(previousTags));
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

    private static long nextBuildNumber(List<AnnotatedTag> previousTags) {
        long max = 0;
        for (AnnotatedTag tag : previousTags) {
            max = Math.max(max, Long.parseLong(tag.buildNumber()));
        }
        return max + 1;
    }

}
