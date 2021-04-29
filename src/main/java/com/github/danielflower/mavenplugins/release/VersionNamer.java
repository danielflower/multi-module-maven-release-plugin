package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.lib.Repository;

import java.util.Collection;

import static java.util.Arrays.asList;

public class VersionNamer {

    /**
     * <p>
     * The delimiter used to append the build number.
     * </p>
     * <p>
     * By default, it will use ".". However, for a three-digit version, a
     * dash ('-') is recommended to be in line with the maven versioning scheme.
     * </p>
     * <p>
     * This can be specified using a command line parameter ("-Ddelimiter=2")
     * or in this plugin's configuration.
     * </p>
     */
    @Parameter(property = "delimiter")
    private String delimiter;

    public VersionNamer(String delimiter) {
        this.delimiter = delimiter;
    }

    public VersionNamer() {
        this(".");
    }

    public VersionName name(String pomVersion, String buildNumber, Collection<String> previousBuildNumbers) throws ValidationException {
        String effectiveBuildNumber = buildNumber;
        if (buildNumber == null) {
            if (previousBuildNumbers == null || previousBuildNumbers.isEmpty()) {
                effectiveBuildNumber = "0";
            } else {
                effectiveBuildNumber = nextBuildNumber(previousBuildNumbers);
            }
        }

        VersionName versionName = new VersionName(pomVersion, pomVersion.replace("-SNAPSHOT", ""), effectiveBuildNumber, this.delimiter);

        if (!Repository.isValidRefName("refs/tags/" + versionName.releaseVersion())) {
            String summary = "Sorry, '" + versionName.releaseVersion() + "' is not a valid version.";
            throw new ValidationException(summary, asList(
                summary,
                "Version numbers are used in the Git tag, and so can only contain characters that are valid in Git tags.",
                "Please see https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html for tag naming rules."
            ));
        }
        return versionName;
    }

    // Increments the largest previous build number. Skips over any non-numeric previous build numbers.
    private static String nextBuildNumber(Collection<String> previousBuildNumbers) {
        long max = 0;
        for (String buildNumber : previousBuildNumbers) {
            try {
                max = Math.max(max, Long.parseLong(buildNumber));
            } catch (NumberFormatException e) {
                // non-numeric build numbers are not unexpected
            }
        }
        return String.valueOf(max + 1);
    }

    String getDelimiter() {
        return delimiter;
    }
}
