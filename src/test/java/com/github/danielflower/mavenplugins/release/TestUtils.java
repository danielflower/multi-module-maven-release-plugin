package com.github.danielflower.mavenplugins.release;

import java.time.ZonedDateTime;

import com.github.danielflower.mavenplugins.release.versioning.ImmutableModuleVersion;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableReleaseInfo;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableFixVersion;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;

public class TestUtils {
    public static ReleaseInfo releaseInfo(long major, long minor, String tagName, String moduleName) {
        final ImmutableReleaseInfo.Builder builder = ImmutableReleaseInfo.builder();
        builder.tagName(tagName);
        final ImmutableModuleVersion.Builder versionBuilder = ImmutableModuleVersion.builder();
        versionBuilder.releaseDate(ZonedDateTime.now());
        versionBuilder.name(moduleName);
        builder.addModules(versionBuilder.version(ImmutableFixVersion.builder().majorVersion(major).minorVersion
                                                                                                        (minor).build
                                                                                                                    ()).build());
        return builder.build();
    }
}
