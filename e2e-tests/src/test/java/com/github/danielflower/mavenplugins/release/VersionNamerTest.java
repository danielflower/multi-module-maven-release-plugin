package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class VersionNamerTest {

    final VersionNamer namer = new VersionNamer();

    @Test
    public void removesTheSnapshotAndSticksTheReleaseVersionOnTheEnd() throws Exception {
        assertThat(namer.name("1.0-SNAPSHOT", "123"), is(equalTo("1.0.123")));
    }

    @Test
    public void throwsIfTheVersionWouldNotBeAValidGitTag() {
        assertThat(errorMessageOf("1.0-SNAPSHOT", "A : yeah /"),
            is(equalTo("Sorry, '1.0.A : yeah /' is not a valid version. Please see https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html for tag naming rules.")));
    }

    private String errorMessageOf(String pomVersion, String releaseVersion) {
        try {
            namer.name(pomVersion, releaseVersion);
            throw new AssertionError("Did not throw an error");
        } catch (MojoExecutionException ex) {
            return ex.getMessage();
        }
    }
}