package com.github.danielflower.mavenplugins.release;

import javax.annotation.Nullable;

import org.apache.maven.project.MavenProject;
import org.immutables.value.Value;

import com.github.danielflower.mavenplugins.release.versioning.ImmutableFixVersion;

@Value.Immutable
public interface ReleasableModule {
    @Nullable
    String getEquivalentVersion();

    @Nullable
    String getNewVersion();

    @Nullable
    String getRelativePathToModule();

    String getArtifactId();

    String getGroupId();

    @Nullable
    MavenProject getProject();

    @Nullable
    Long getVersion();

    @Nullable
    ImmutableFixVersion versionInfo();

    default boolean willBeReleased() {
        return getEquivalentVersion() == null;
    }

    default String getVersionToDependOn() {
        return willBeReleased() ? versionInfo().versionAsString() : getEquivalentVersion();
    }

    default ImmutableReleasableModule createReleasableVersion() {
        final ImmutableReleasableModule.Builder builder = ImmutableReleasableModule.builder().from(this);
        builder.equivalentVersion(null);
        return builder.build();
    }
}
