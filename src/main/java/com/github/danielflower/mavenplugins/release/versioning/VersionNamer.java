package com.github.danielflower.mavenplugins.release.versioning;

import java.util.Optional;

import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.ValidationException;

public class VersionNamer {

    public static final String PREVIOUS_BUILDS_REQUIRED       = "Previous build number required when creating bugfix " + "release.";

    private final boolean     bugfixRelease;
    private final ReleaseInfo previousRelease;

    public VersionNamer(boolean bugfixRelease, ReleaseInfo previousRelease) {
        this.bugfixRelease = bugfixRelease;
        this.previousRelease = previousRelease;
    }

    /**
     * @return next version name to use
     *
     * @throws ValidationException
     */
    public FixVersion nextVersion(MavenProject project) throws ValidationException {

        final Optional<ModuleVersion> previousVersion = previousRelease.versionForModule(project.getArtifactId());
        if (previousVersion.isPresent()) {
            return followupVersion(previousVersion.get());
        } else {
            return initialVersion(project);
        }
    }

    private FixVersion initialVersion(MavenProject project) {
        if (bugfixRelease) {
            throw new ValidationException(PREVIOUS_BUILDS_REQUIRED);
        }
        final SnapshotVersion currentSnapshot = new VersionMatcher(project.getVersion()).snapshotVersion();
        return ImmutableFixVersion.builder().majorVersion(currentSnapshot.getMajorVersion()).minorVersion(0).build();
    }

    private FixVersion followupVersion(ModuleVersion previousModule) {

        final ImmutableFixVersion.Builder builder = ImmutableFixVersion.builder();
        FixVersion previousVersion = previousModule.getVersion();
        builder.from(previousVersion);
        if (bugfixRelease) {
            builder.bugfixVersion(previousVersion.getBugfixVersion().orElse(0L) + 1);
        } else {
            if (previousVersion.getBugfixVersion().isPresent()) {
                throw new IllegalStateException("can only create bugfixes from bugfix branch");
            }
            builder.minorVersion(previousVersion.getMinorVersion() + 1);
        }
        return builder.build();
    }
}
