package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.lib.Repository;

import java.util.List;

import static java.util.Arrays.asList;

public class VersionNamer {

    public VersionName name(String pomVersion, Long buildNumber, List<AnnotatedTag> previousTags) throws ValidationException {
        if (buildNumber == null) {
            if (previousTags == null || previousTags.size() == 0) {
                buildNumber = Long.valueOf(0);
            } else {
                buildNumber = nextBuildNumber(previousTags);
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
            max = Math.max(max, tag.buildNumber());
        }
        return max + 1;
    }

}
