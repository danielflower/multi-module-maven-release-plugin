package com.github.danielflower.mavenplugins.release.versioning;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class VersionMatcherTest {

    @Test(expected = IllegalArgumentException.class)
    public void illegalPatternFix() {
        fixVersion("hello");
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalPatternSnapshot() {
        snapshotVersion("hello");
    }

    @Test
    public void parseSnapshot() {
        assertThat(snapshotVersion("1-SNAPSHOT").getMajorVersion(), is(1L));
    }

    @Test
    public void parseFix() {
        final ImmutableFixVersion fixVersion = ImmutableFixVersion.builder().majorVersion(1).minorVersion(23).build();
        assertThat(fixVersion("1.23"), is(fixVersion));
    }

    @Test
    public void parseFixWithBugfix() {
        final ImmutableFixVersion fixVersion = ImmutableFixVersion.builder().majorVersion(1).minorVersion(23)
                                                                  .bugfixVersion(456).build();
        assertThat(fixVersion("1.23.456"), is(fixVersion));
    }

    private FixVersion fixVersion(String versionString) {
        return new VersionMatcher(versionString).fixVersion();
    }

    private SnapshotVersion snapshotVersion(String versionString) {
        return new VersionMatcher(versionString).snapshotVersion();
    }
}