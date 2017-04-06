package e2e;

import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.CountMatcher.noneOf;
import static scaffolding.CountMatcher.oneOf;
import static scaffolding.MvnRunner.assertArtifactInLocalRepo;
import static scaffolding.MvnRunner.assertArtifactNotInLocalRepo;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.TestUtils;

public class IndependentVersionsBugfixTest {

    private static final String GROUP_ID = TestUtils.TEST_GROUP_ID + ".independentversions";
    public static final String INDEPENDENT_VERSIONS_ARTIFACT = "independent-versions";
    public static final String CORE_UTILS_ARTIFACT = "core-utils";
    public static final String CONSOLE_APP_ARTIFACT = "console-app";
    private final        String expectedParentVersion = "1.0";
    private final        String expectedCoreVersion   = "2.0";
    private final        String expectedAppVersion    = "3.0";
    private              String branchName            = "bugfix-test";

    @Rule
    public TestProject testProject = new TestProject(ProjectType.INDEPENDENT_VERSIONS_BUGFIX);

    @Before
    public void releaseProject() throws Exception {
        assertArtifactNotInLocalRepo(GROUP_ID, INDEPENDENT_VERSIONS_ARTIFACT, expectedParentVersion);
        assertArtifactNotInLocalRepo(GROUP_ID, CORE_UTILS_ARTIFACT, expectedCoreVersion);
        assertArtifactNotInLocalRepo(GROUP_ID, CONSOLE_APP_ARTIFACT, expectedAppVersion);
        testProject.mvnRelease();
        testProject.local.branchCreate().setName(branchName).call();
    }

    @Test
    public void checkReleasesBeforeBugfix() throws Exception {
        assertArtifactInLocalRepo(GROUP_ID, INDEPENDENT_VERSIONS_ARTIFACT, expectedParentVersion);
        assertArtifactInLocalRepo(GROUP_ID, CORE_UTILS_ARTIFACT, expectedCoreVersion);
        assertArtifactInLocalRepo(GROUP_ID, CONSOLE_APP_ARTIFACT, expectedAppVersion);
    }

    @Test
    public void createBugfixReleaseAll() throws Exception {
        testProject.local.checkout().setName(branchName).call();
        testProject.mvnReleaseBugfix();
        assertArtifactInLocalRepo(GROUP_ID, INDEPENDENT_VERSIONS_ARTIFACT, expectedParentVersion + ".1");
        assertArtifactInLocalRepo(GROUP_ID, CORE_UTILS_ARTIFACT, expectedCoreVersion + ".1");
        assertArtifactInLocalRepo(GROUP_ID, CONSOLE_APP_ARTIFACT, expectedAppVersion + ".1");
    }

    @Test
    public void createBugfixReleaseAllAndIgnoreNextMasterRelease() throws Exception {
        testProject.mvnRelease();
        testProject.local.pull();
        testProject.local.checkout().setName(branchName).call();
        testProject.mvnReleaseBugfix();
        assertArtifactInLocalRepo(GROUP_ID, INDEPENDENT_VERSIONS_ARTIFACT, expectedParentVersion + ".1");
        assertArtifactInLocalRepo(GROUP_ID, CORE_UTILS_ARTIFACT, expectedCoreVersion + ".1");
        assertArtifactInLocalRepo(GROUP_ID, CONSOLE_APP_ARTIFACT, expectedAppVersion + ".1");
    }

    @Test
    public void releaseOnlyModulesThatHaveChanged() throws Exception {
        testProject.local.checkout().setName(branchName).call();
        testProject.commitRandomFile(CONSOLE_APP_ARTIFACT);
        testProject.mvnReleaseBugfix();
        assertArtifactNotInLocalRepo(GROUP_ID, INDEPENDENT_VERSIONS_ARTIFACT, expectedParentVersion + ".1");
        assertArtifactNotInLocalRepo(GROUP_ID, CORE_UTILS_ARTIFACT, expectedCoreVersion + ".1");
        assertArtifactInLocalRepo(GROUP_ID, CONSOLE_APP_ARTIFACT, expectedAppVersion + ".1");
    }

    @Test
    public void buildOnlyModulesThatHaveChanged() throws Exception {
        testProject.local.checkout().setName(branchName).call();
        testProject.commitRandomFile(CONSOLE_APP_ARTIFACT);
        final List<String> outputLines = testProject.mvnReleaseBugfix();
        assertThat(outputLines, oneOf(containsString("Building console-app 3.0.1")));
        assertThat(outputLines, noneOf(containsString("Building core-utils")));
        assertThat(outputLines, noneOf(containsString("Building independent-versions 1.0")));
    }

    @Test
    public void createBugfixCoreChanged() throws Exception {
        testProject.local.checkout().setName(branchName).call();
        testProject.commitRandomFile(CORE_UTILS_ARTIFACT);
        testProject.mvnReleaseBugfix();
        assertArtifactInLocalRepo(GROUP_ID, INDEPENDENT_VERSIONS_ARTIFACT, expectedParentVersion);
        assertArtifactInLocalRepo(GROUP_ID, CORE_UTILS_ARTIFACT, expectedCoreVersion + ".1");
        assertArtifactInLocalRepo(GROUP_ID, CONSOLE_APP_ARTIFACT, expectedAppVersion + ".1");
    }

    @Test
    public void createSecondBugfixReleaseAll() throws Exception {
        testProject.local.checkout().setName(branchName).call();
        testProject.mvnReleaseBugfix();
        testProject.mvnReleaseBugfix();
        assertArtifactInLocalRepo(GROUP_ID, INDEPENDENT_VERSIONS_ARTIFACT, expectedParentVersion + ".2");
        assertArtifactInLocalRepo(GROUP_ID, CORE_UTILS_ARTIFACT, expectedCoreVersion + ".2");
        assertArtifactInLocalRepo(GROUP_ID, CONSOLE_APP_ARTIFACT, expectedAppVersion + ".2");
    }

}
