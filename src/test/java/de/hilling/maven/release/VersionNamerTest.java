package de.hilling.maven.release;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import de.hilling.maven.release.versioning.FixVersion;
import de.hilling.maven.release.versioning.ReleaseInfo;
import de.hilling.maven.release.versioning.VersionNamer;

public class VersionNamerTest {

    private static final String MODULE_NAME = "test-module";
    private VersionNamer namer;
    private VersionNamer bugfixNamer;
    private MavenProject testProject;

    @Before
    public void setUp() {
        testProject = new MavenProject();
        testProject.setVersion("1-SNAPSHOT");
        testProject.setGroupId(TestUtils.TEST_GROUP_ID);
        testProject.setArtifactId(MODULE_NAME);
        final ReleaseInfo previousRelease = TestUtils.releaseInfo(1, 5, "test", MODULE_NAME);
        namer = new VersionNamer(false, previousRelease);
        bugfixNamer = new VersionNamer(true, previousRelease);
    }

    @Test
    public void removesTheSnapshotAndChoosesNextMinorForNextRelease() throws Exception {
        final FixVersion next = namer.nextVersion(testProject);
        assertThat(next.getMajorVersion(), is(1L));
        assertThat(next.getMinorVersion(), is(6L));
    }

    @Test
    public void removesTheSnapshotAndSticksTheBuildNumberOnTheEndIllegalVersion() throws Exception {
        assertThat(errorMessageOf("1.0-SNAPSHOT", namer),
                   hasItems("Snapshot version must match ^(?<major>\\d+)-SNAPSHOT$"));
    }

    @Test
    public void removesTheSnapshotAndSticksTheBuildNumberOnTheEnd() throws Exception {
        assertThat(namer.nextVersion(testProject).toString(), is(equalTo("1.6")));
    }

    @Test
    public void ifTheBuildNumberIsNullAndThePreviousBuildNumbersIsEmptyListThenZeroIsUsedBugfixSupportOn() throws
                                                                                                           Exception {
        bugfixNamer = new VersionNamer(true, TestUtils.releaseInfo(1, 5, "test", "some-mod"));
        assertThat(errorMessageOf("1-SNAPSHOT", bugfixNamer), hasItems(VersionNamer.PREVIOUS_BUILDS_REQUIRED));
    }

    @Test
    public void ifTheBuildNumberIsNullButThereIsAPreviousBuildNumbersThenThatValueIsIncrementedSecondBugfixRelease() throws
                                                                                                                     Exception {
        assertThat(bugfixNamer.nextVersion(testProject).toString(), is(equalTo("1.5.1")));
    }

    @Test
    public void throwsIfTheVersionWouldNotBeAValidGitTag() {
        assertThat(errorMessageOf("1.0-A : yeah /-SNAPSHOT", namer),
                   hasItems("Snapshot version must match ^(?<major>\\d+)-SNAPSHOT$"));
    }

    private List<String> errorMessageOf(String pomVersion, VersionNamer namer) {
        try {
            testProject.setVersion(pomVersion);
            namer.nextVersion(testProject);
            throw new AssertionError("Did not throw an error");
        } catch (ValidationException ex) {
            return ex.getMessages();
        }
    }
}