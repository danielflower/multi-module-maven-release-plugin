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

    public Optional<ImmutableModuleVersion> versionForModule(String moduleName) {
        return getModules().stream().filter(mv -> mv.getName().equals(moduleName)).findFirst();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getTagName()).append(" ");
        for (ImmutableModuleVersion moduleVersion : getModules()) {
            builder.append(moduleVersion.toString());
        }
        return builder.toString();
    }

    public boolean isEmpty() {
        return getModules().isEmpty();
    }
}
