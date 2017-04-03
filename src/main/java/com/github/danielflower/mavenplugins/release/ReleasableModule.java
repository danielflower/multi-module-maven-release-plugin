package com.github.danielflower.mavenplugins.release;

import org.apache.maven.project.MavenProject;
import org.immutables.value.Value;

import com.github.danielflower.mavenplugins.release.versioning.ImmutableFixVersion;

@Value.Immutable
public interface ReleasableModule {
    String getRelativePathToModule();

    MavenProject getProject();

    ImmutableFixVersion getVersion();

    boolean isToBeReleased();
}
