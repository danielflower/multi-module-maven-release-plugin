package com.github.danielflower.mavenplugins.release;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.fail;

@RunWith(Enclosed.class)
public class VersionNamerTest {

    @RunWith(Parameterized.class)
    public static class Name {
        @Parameter
        public String developmentVersion;

        @Parameter(1)
        public String buildNumber;

        @Parameter(2)
        public Collection<String> previousBuildNumbers;

        @Parameter(3)
        public String delimiter;

        @Parameter(4)
        public String expectedReleaseVersion;

        @Parameter(5)
        public boolean expectException;

        @Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(
                new Object[]{"1.0-SNAPSHOT", "123", null, null, "1.0.123", false},
                new Object[]{"1.0-SNAPSHOT", null, new ArrayList<String>(), null, "1.0.0", false},
                new Object[]{"1.0-SNAPSHOT", null, asList("9", "10", "8"), null, "1.0.11", false},
                new Object[]{"1.0-SNAPSHOT", null, asList("1", "2", "A", "B"), null, "1.0.3", false},
                new Object[]{"1.0.0-SNAPSHOT", "123", null, "-", "1.0.0-123", false},
                new Object[]{"1.0-A : yeah /-SNAPSHOT", "0", null, null, "1.0-A : yeah /.0", true}
            );
        }

        @Test
        public void shouldConsiderPreviousBuildNumbers() {
            // given
            VersionNamer namer;
            if (delimiter == null) {
                namer = new VersionNamer();
            } else {
                namer = new VersionNamer(delimiter);
            }

            try {
                // when
                VersionName name = namer.name(developmentVersion, buildNumber, previousBuildNumbers);
                if (expectException) {
                    fail(String.format("Expected exception for development version '%s' but got none.", developmentVersion));
                }
                // then
                assertThat(name.releaseVersion(), is(expectedReleaseVersion));
            } catch (ValidationException e) {
                assertThat(e.getMessages(), hasSize(3));
                assertThat(e.getMessages(), hasItems(
                    String.format("Sorry, '%s' is not a valid version.", expectedReleaseVersion),
                    "Please see https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html for tag naming rules."
                ));
            }
        }
    }

    public static class GetDelimiter {
        @Test
        public void shouldReturnDefinedDelimiterOrDotPerDefault() {
            assertThat(new VersionNamer("-").getDelimiter(), is("-"));
            assertThat(new VersionNamer("@").getDelimiter(), is("@"));
            assertThat(new VersionNamer().getDelimiter(), is("."));
        }
    }
}
