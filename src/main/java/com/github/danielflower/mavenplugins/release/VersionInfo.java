package com.github.danielflower.mavenplugins.release;

import org.immutables.value.Value;

@Value.Immutable
public interface VersionInfo {
    /**
     * @return the last number of the automatically created version.
     */
    Long getBuildNumber();

    /**
     * @return the number before the build number. Used for bugfix releases.
     */
    Long getBugfixBranchNumber();
}
