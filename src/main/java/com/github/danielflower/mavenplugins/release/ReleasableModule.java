package com.github.danielflower.mavenplugins.release;

import org.apache.maven.project.MavenProject;
import org.immutables.value.Value;

import com.github.danielflower.mavenplugins.release.versioning.ImmutableModuleVersion;

@Value.Immutable
public interface ReleasableModule {
    String getRelativePathToModule();

    MavenProject getProject();

    ImmutableModuleVersion getImmutableModule();

    boolean isToBeReleased();
}
