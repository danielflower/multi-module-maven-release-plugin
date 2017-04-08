package de.hilling.maven.release;

import org.apache.maven.project.MavenProject;
import org.immutables.value.Value;

import de.hilling.maven.release.versioning.ImmutableModuleVersion;

@Value.Immutable
public interface ReleasableModule {
    String getRelativePathToModule();

    MavenProject getProject();

    ImmutableModuleVersion getImmutableModule();

    boolean isToBeReleased();
}
