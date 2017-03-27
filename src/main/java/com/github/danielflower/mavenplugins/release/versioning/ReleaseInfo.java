package com.github.danielflower.mavenplugins.release.versioning;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public interface ReleaseInfo {

    /**
     * @return name of the tag corresponding to this info. empty if no release exists.
     */
    Optional<String> getTagName();

    List<ModuleVersion> getModules();

    default Optional<ModuleVersion> versionForModule(String moduleName) {
        return getModules().stream().filter(mv -> mv.getName().equals(moduleName)).findFirst();
    }
}
