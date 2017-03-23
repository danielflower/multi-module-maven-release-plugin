package com.github.danielflower.mavenplugins.release;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.util.Collection;

import org.eclipse.jgit.lib.Repository;

public class VersionNamer {

    public static final String PREVIOUS_BUILDS_REQUIRED       = "Previous build number required when creating bugfix " + "release.";
    public static final String SINGLE_VERSION_NUMBER_REQUIRED = "Build Numbers must match pattern '[0-9]+-SNAPSHOT' when using bugfix release feature for " + "creation of bugfix tags.";
    private final boolean bugfixRelease;

    public VersionNamer(boolean bugfixRelease) {
        this.bugfixRelease = bugfixRelease;
    }

    private VersionInfo nextBuildVersion(Collection<VersionInfo> previousBuildNumbers) {
        final VersionInfo maxVersion = java.util.Collections.max(previousBuildNumbers);
        if (bugfixRelease) {
            if (maxVersion.getBugfixBranchNumber() == null) {
                // first bugfix release
                System.err.println("first bugfix from " + maxVersion);
                return new VersionInfo(maxVersion.getBuildNumber(), 1L);
            } else {
                // following bugfix releases
                System.err.println("followup bugfix from " + maxVersion);
                return new VersionInfo(maxVersion.getBugfixBranchNumber(), maxVersion.getBuildNumber() + 1);
            }
        } else {
            System.err.println("no bugfix");
            return new VersionInfo(maxVersion.getBugfixBranchNumber(), maxVersion.getBuildNumber() + 1);
        }
    }

    /**
     * @param pomVersion           current pomVersion, including -SNAPSHOT
     * @param buildNumber          forced build number. May be null.
     * @param previousBuildNumbers all previous build numbers on the current branch.
     *
     * @return next version name to use
     *
     * @throws ValidationException
     */
    public VersionName name(String pomVersion, Long buildNumber, Collection<VersionInfo> previousBuildNumbers) throws
                                                                                                        ValidationException {

        if (bugfixRelease) {
            if (previousBuildNumbers.isEmpty()) {
                throw new ValidationException("illegal bugfix release setup", singletonList(PREVIOUS_BUILDS_REQUIRED));
            }
            if (!pomVersion.matches("[0-9]+-SNAPSHOT")) {
                throw new ValidationException("illegal bugfix release setup", singletonList(SINGLE_VERSION_NUMBER_REQUIRED));
            }
        }

        VersionInfo nextVersion;

        if (buildNumber == null) {
            if (previousBuildNumbers == null || previousBuildNumbers.size() == 0) {
                nextVersion = new VersionInfo(null, 0L);
            } else {
                nextVersion = nextBuildVersion(previousBuildNumbers);
            }
        } else {
            nextVersion = new VersionInfo(null, buildNumber);
        }

        VersionName versionName = new VersionName(pomVersion, pomVersion.replace("-SNAPSHOT", ""), nextVersion);

        if (!Repository.isValidRefName("refs/tags/" + versionName.releaseVersion())) {
            String summary = "Sorry, '" + versionName.releaseVersion() + "' is not a valid version.";
            throw new ValidationException(summary, asList(summary,
                                                          "Version numbers are used in the Git tag, and so can only contain characters that are valid in git tags.",
                                                          "Please see https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html for tag naming rules."));
        }
        return versionName;
    }
}
