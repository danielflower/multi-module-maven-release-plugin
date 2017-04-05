package com.github.danielflower.mavenplugins.release.versioning;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public abstract class ReleaseInfo {

    /**
     * @return name of the tag corresponding to this info. empty if no release exists.
     */
    public abstract Optional<String> getTagName();

    public abstract List<ImmutableModuleVersion> getModules();

    public Optional<ImmutableModuleVersion> versionForArtifact(QualifiedArtifact artifact) {
        return getModules().stream().filter(mv -> mv.getArtifact().equals(artifact)).findFirst();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        if (getTagName().isPresent()) {
            builder.append("tag '").append(getTagName().get()).append("' ");
        } else {
            builder.append("(no tag yet) ");
        }
        for (ImmutableModuleVersion moduleVersion : getModules()) {
            builder.append(moduleVersion.toString());
        }
        return builder.toString();
    }

    public boolean isEmpty() {
        return getModules().isEmpty();
    }
}
