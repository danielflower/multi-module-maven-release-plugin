package com.github.danielflower.mavenplugins.release;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class VersionNamerTest {

    private VersionNamer namer;
    private VersionNamer bugfixNamer;

    @Before
    public void setUp() {
        namer = new VersionNamer(false);
        bugfixNamer = new VersionNamer(true);
    }

    @Test
    public void removesTheSnapshotAndSticksTheBuildNumberOnTheEnd() throws Exception {
        assertThat(namer.name("1.0-SNAPSHOT", 123L, null).releaseVersion(), is(equalTo("1.0.123")));
    }

    @Test
    public void removesTheSnapshotAndSticksTheBuildNumberOnTheEndBugfixActive() throws Exception {
        assertThat(errorMessageOf("1.0-SNAPSHOT", 123L, bugfixNamer, createVersionList(2L)),
                   hasItems(
                       VersionNamer.SINGLE_VERSION_NUMBER_REQUIRED
                   )
        );
    }

    @Test
    public void removesTheSnapshotAndSticksTheBuildNumberOnTheEndBugfixSupport() throws Exception {
        assertThat(namer.name("1-SNAPSHOT", 123L, null).releaseVersion(), is(equalTo("1.123")));
    }

    @Test
    public void ifTheBuildNumberIsNullAndThePreviousBuildNumbersIsEmptyListThenZeroIsUsed() throws Exception {
        assertThat(namer.name("1.0-SNAPSHOT", null, new ArrayList<VersionInfo>()).releaseVersion(), is(equalTo("1.0.0")));
    }

    @Test
    public void ifTheBuildNumberIsNullAndThePreviousBuildNumbersIsEmptyListThenZeroIsUsedBugfixSupport() throws Exception {
        assertThat(namer.name("1-SNAPSHOT", null, new ArrayList<VersionInfo>()).releaseVersion(), is(equalTo("1.0")));
    }

    @Test
    public void ifTheBuildNumberIsNullAndThePreviousBuildNumbersIsEmptyListThenZeroIsUsedBugfixSupportOn() throws
                                                                                                          Exception {
        assertThat(errorMessageOf("1-SNAPSHOT", null, bugfixNamer, new ArrayList<VersionInfo>()),
                   hasItems(
                       VersionNamer.PREVIOUS_BUILDS_REQUIRED
                   )
        );
    }

    @Test
    public void ifTheBuildNumberIsNullButThereIsAPreviousBuildNumbersThenThatValueIsIncremented() throws Exception {
        assertThat(namer.name("1.0-SNAPSHOT", null, createVersionList(9L, 10L, 8L)).releaseVersion(), is(equalTo
                                                                                                      ("1.0.11")));
    }

    private Collection<VersionInfo> createVersionList(Long ... numbers) {
        final ArrayList<VersionInfo> versionInfos = new ArrayList<>();
        for (Long number : numbers) {
            versionInfos.add(new VersionInfo(number, null));
        }

        return versionInfos;
    }

    @Test
    public void ifTheBuildNumberIsNullButThereIsAPreviousBuildNumbersThenThatValueIsIncrementedBugfixSupport() throws Exception {
        assertThat(namer.name("1-SNAPSHOT", null, createVersionList(9L, 10L, 8L)).releaseVersion(), is(equalTo("1.11")));
    }

    @Test
    public void ifTheBuildNumberIsNullButThereIsAPreviousBuildNumbersThenThatValueIsIncrementedBugfixSupportActive()
        throws Exception {
        assertThat(bugfixNamer.name("1-SNAPSHOT", null, createVersionList(9L, 10L, 8L)).releaseVersion(), is(equalTo
                                                                                                      ("1.10.1")));
    }

    @Test
    public void
    ifTheBuildNumberIsNullButThereIsAPreviousBuildNumbersThenThatValueIsIncrementedSecondBugfixRelease()
        throws Exception {
        assertThat(bugfixNamer.name("1-SNAPSHOT", null, createVersionList(9L, 10L, 8L)).releaseVersion(), is(equalTo
                                                                                                      ("1.10.1")));
    }

    @Test
    public void throwsIfTheVersionWouldNotBeAValidGitTag() {
        assertThat(errorMessageOf("1.0-A : yeah /-SNAPSHOT", 0L, namer, null),
            hasItems(
                "Sorry, '1.0-A : yeah /.0' is not a valid version.",
                "Please see https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html for tag naming rules."
            )
        );
    }


    private List<String> errorMessageOf(String pomVersion, Long buildNumber, VersionNamer namer,
                                        Collection<VersionInfo> previousBuildNumbers) {
        try {

            namer.name(pomVersion, buildNumber, previousBuildNumbers);
            throw new AssertionError("Did not throw an error");
        } catch (ValidationException ex) {
            return ex.getMessages();
        }
    }
}