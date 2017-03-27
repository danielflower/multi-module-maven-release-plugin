package com.github.danielflower.mavenplugins.release.versioning;

import java.time.ZonedDateTime;

import org.immutables.value.Value;

@Value.Immutable
public interface ModuleVersion {

    ZonedDateTime getReleaseDate();

    String getName();

    FixVersion getVersion();
}
