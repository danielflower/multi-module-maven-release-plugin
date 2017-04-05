package com.github.danielflower.mavenplugins.release.versioning;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.danielflower.mavenplugins.release.ValidationException;

public class VersionMatcher {
    private static final Pattern SNAPSHOT_PATTERN = Pattern.compile("^(?<major>\\d+)-SNAPSHOT$");
    private static final Pattern FIX_PATTERN = Pattern.compile("^(?<major>\\d+)\\.(?<minor>\\d+)(\\.(?<bugfix>\\d+))" +
                                                                   "?$");

    private final String version;

    public VersionMatcher(String version) {
        this.version = version;
    }

    public SnapshotVersion snapshotVersion() {
        Matcher matcher = SNAPSHOT_PATTERN.matcher(version);
        if (matcher.matches()) {
            final ImmutableSnapshotVersion.Builder builder = ImmutableSnapshotVersion.builder();
            return builder.majorVersion(fromMatcherGroup(matcher, "major")).build();
        } else {
            throw new ValidationException("Snapshot version must match "  + SNAPSHOT_PATTERN.pattern());
        }
    }

    public long fromMatcherGroup(Matcher matcher, String group) {
        return Long.parseLong(matcher.group(group));
    }

    public ImmutableFixVersion fixVersion() {
        Matcher matcher = FIX_PATTERN.matcher(version);
        if (matcher.matches()) {
            final ImmutableFixVersion.Builder builder = ImmutableFixVersion.builder();
            builder.majorVersion(fromMatcherGroup(matcher, "major"));
            builder.minorVersion(fromMatcherGroup(matcher, "minor"));
            if (matcher.group("bugfix") !=null) {
                builder.bugfixVersion(fromMatcherGroup(matcher, "bugfix"));
            }
            return builder.build();
        } else {
            throw new ValidationException("Fix version must match "  + FIX_PATTERN.pattern());
        }

    }
}
