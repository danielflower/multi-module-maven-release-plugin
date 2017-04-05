package de.hilling.maven.release.versioning;

import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public abstract class FixVersion {
    /**
     * @return Major version as used in maven dependencies.
     */
    public abstract long getMajorVersion();

    /**
     * @return Minor version as used in maven dependencies.
     */
    public abstract long getMinorVersion();

    /**
     * @return Bugfix version as used in maven dependencies.
     */
    public abstract Optional<Long> getBugfixVersion();

    @Override
    public String toString() {
        return getBugfixVersion().map(bugfix -> getMajorVersion() + "." + getMinorVersion() + "." + bugfix)
                                 .orElseGet(() -> getMajorVersion() + "." + getMinorVersion());
    }

}
