package com.github.danielflower.mavenplugins.release;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class VersionNamerTest {

    final VersionNamer namer = new VersionNamer();

    @Test
    public void removesTheSnapshotAndSticksTheBuildNumberOnTheEnd() throws Exception {
        assertThat(namer.name("1.0-SNAPSHOT", "123", null).releaseVersion(), is(equalTo("1.0.123")));
    }

    @Test
    public void ifTheBuildNumberIsNullAndThePreviousTagIsNullThenZeroIsUsed() throws Exception {
        assertThat(namer.name("1.0-SNAPSHOT", null, null).releaseVersion(), is(equalTo("1.0.0")));
    }

    @Test
    public void ifTheBuildNumberIsNullButThereIsAPreviousTagThenThatValueIsIncremented() throws Exception {
        assertThat(namer.name("1.0-SNAPSHOT", null, AnnotatedTag.create("something", "1.0", "10")).releaseVersion(), is(equalTo("1.0.11")));
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


    private List<String> errorMessageOf(String pomVersion, String buildNumber) {
        try {
            namer.name(pomVersion, buildNumber, null);
            throw new AssertionError("Did not throw an error");
        } catch (ValidationException ex) {
            return ex.getMessages();
        }
    }
}