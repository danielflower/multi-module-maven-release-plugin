package com.github.danielflower.mavenplugins.release.versioning;

import org.immutables.value.Value;

/**
 * Maven identifier:
 * groupid and artifact id.
 */
@Value.Immutable
public abstract class QualifiedArtifact {

    public abstract String getGroupId();

    public abstract String getArtifactId();

    @Override
    public String toString() {
        return getGroupId() + ":" + getArtifactId();
    }
}
