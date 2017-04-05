package com.github.danielflower.mavenplugins.release.versioning;

import java.time.ZonedDateTime;

import org.immutables.value.Value;

@Value.Immutable
public abstract class ModuleVersion {

    public abstract ZonedDateTime getReleaseDate();

    public abstract String getReleaseTag();

    public abstract ImmutableQualifiedArtifact getArtifact();

    public abstract ImmutableFixVersion getVersion();

    @Override
    public String toString() {
        return getArtifact() + "-"  + getVersion().toString() + "-" + getReleaseTag();
    }
}
