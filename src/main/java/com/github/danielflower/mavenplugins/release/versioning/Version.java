package com.github.danielflower.mavenplugins.release.versioning;

import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public interface Version {
    /**
     * @return Major version as used in maven dependencies.
     */
    long getMajorVersion();

    /**
     * @return Minor version as used in maven dependencies.
     */
    long getMinorVersion();

    /**
     * @return Bugfix version as used in maven dependencies.
     */
    Optional<Long> getBugfixVersion();

}
