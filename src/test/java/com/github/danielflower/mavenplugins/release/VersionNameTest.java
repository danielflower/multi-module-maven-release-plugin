package com.github.danielflower.mavenplugins.release;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class VersionNameTest {

    @Parameter
    public String version;

    @Parameter(1)
    public String buildNumber;

    @Parameter(2)
    public String expectedReleaseVersion;

    @Parameters(name = "{0} {1} -> {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new String[]{"1.0", "5", "1.0-5"},
            new String[]{"1.0", "", "1.0"}
            //new String[]{"1.0", null, "1.0"}, // in theory yes, in practice there's a not-null precondition in the VersionName c'tor
        );
    }

    @Test
    public void shouldAppendBuildNumberIfNotEmpty() {
        // given
        VersionName versionName = new VersionName("1.0-SNAPSHOT", version, buildNumber, "-");

        // when
        String releaseVersion = versionName.releaseVersion();

        // then
        assertThat(releaseVersion, is(expectedReleaseVersion));
    }
}
