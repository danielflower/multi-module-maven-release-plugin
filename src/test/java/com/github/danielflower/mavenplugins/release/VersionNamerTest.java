package com.github.danielflower.mavenplugins.release;

import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class VersionNamerTest {


    private final Clock clock = new Clock() {
        @Override
        public Date now() {
            return new Date(1422539966749L);
        }
    };
    final VersionNamer namer = new VersionNamer(clock);

    @Test
    public void removesTheSnapshotAndSticksTheBuildNumberOnTheEnd() throws Exception {
        assertThat(namer.name("1.0-SNAPSHOT", "123").fullVersion(), is(equalTo("1.0.123")));
    }

    @Test
    public void throwsIfTheVersionWouldNotBeAValidGitTag() {
        assertThat(errorMessageOf("1.0-SNAPSHOT", "A : yeah /"),
            hasItems(
                "Sorry, '1.0.A : yeah /' is not a valid version.",
                "Please see https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html for tag naming rules."
            )
        );
    }

    @Test
    public void usesTheCurrentDateAsABuildNumberIfNoneIsSpecified() throws ValidationException {
        String expected = "1.0.20150129135926";
        assertThat(namer.name("1.0-SNAPSHOT", null).fullVersion(), is(equalTo(expected)));
        assertThat(namer.name("1.0-SNAPSHOT", "").fullVersion(), is(equalTo(expected)));
        assertThat(namer.name("1.0-SNAPSHOT", " \t\r\n").fullVersion(), is(equalTo(expected)));
    }

    private List<String> errorMessageOf(String pomVersion, String buildNumber) {
        try {
            namer.name(pomVersion, buildNumber);
            throw new AssertionError("Did not throw an error");
        } catch (ValidationException ex) {
            return ex.getMessages();
        }
    }
}