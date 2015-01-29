package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.lib.Repository;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import static java.util.Arrays.asList;

public class VersionNamer {
    private final Clock clock;

    public VersionNamer(Clock clock) {
        this.clock = clock;
    }

    public String name(String pomVersion, String buildNumber) throws ValidationException {
        if (buildNumber == null || buildNumber.trim().length() == 0) {
            buildNumber = currentDate();
        }

        String newVersion = pomVersion.replace("-SNAPSHOT", "").concat(".").concat(buildNumber);
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

    private String currentDate() {
        DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(clock.now());
    }
}
