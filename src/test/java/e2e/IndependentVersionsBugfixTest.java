package e2e;

import scaffolding.TestProject;

import static scaffolding.MvnRunner.assertArtifactInLocalRepo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class IndependentVersionsBugfixTest {

    private static final String GROUP_ID              = "com.github.danielflower.mavenplugins.testprojects.independentversions";
    private final        String expectedParentVersion = "1.0";
    private final        String expectedCoreVersion   = "2.0";
    private final        String expectedAppVersion    = "3.0";
    private              String branchName            = "bugfix-test";

    @Rule
    public TestProject testProject = new TestProject(ProjectType.INDEPENDENT_VERSIONS_BUGFIX);

    @Before
    public void releaseProject() throws Exception {
        testProject.mvnRelease();
        testProject.origin.branchCreate().setName(branchName).call();
    }

    @Test
    public void checkReleasesBeforeBugfix() throws Exception {
        assertArtifactInLocalRepo(GROUP_ID, "independent-versions", expectedParentVersion);
        assertArtifactInLocalRepo(GROUP_ID, "core-utils", expectedCoreVersion);
        assertArtifactInLocalRepo(GROUP_ID, "console-app", expectedAppVersion);
    }

    @Test
    public void createBugfixReleaseAll() throws Exception {
        testProject.origin.checkout().setName(branchName).call();
        testProject.mvnReleaseBugfix();
        assertArtifactInLocalRepo(GROUP_ID, "independent-versions", expectedParentVersion + ".1");
        assertArtifactInLocalRepo(GROUP_ID, "core-utils", expectedCoreVersion + ".1");
        assertArtifactInLocalRepo(GROUP_ID, "console-app", expectedAppVersion + ".1");
    }

    @Test
    public void createBugfixAppChanged() throws Exception {
        testProject.origin.checkout().setName(branchName).call();
        testProject.commitRandomFile("console-app");
        testProject.mvnReleaseBugfix();
        assertArtifactInLocalRepo(GROUP_ID, "independent-versions", expectedParentVersion);
        assertArtifactInLocalRepo(GROUP_ID, "core-utils", expectedCoreVersion);
        assertArtifactInLocalRepo(GROUP_ID, "console-app", expectedAppVersion + ".1");
    }

    @Test
    public void createBugfixCoreChanged() throws Exception {
        testProject.origin.checkout().setName(branchName).call();
        testProject.commitRandomFile("core-utils");
        testProject.mvnReleaseBugfix();
        assertArtifactInLocalRepo(GROUP_ID, "independent-versions", expectedParentVersion);
        assertArtifactInLocalRepo(GROUP_ID, "core-utils", expectedCoreVersion + ".1");
        assertArtifactInLocalRepo(GROUP_ID, "console-app", expectedAppVersion + ".1");
    }

    @Test
    public void createSecondBugfixReleaseAll() throws Exception {
        testProject.origin.checkout().setName(branchName).call();
        testProject.mvnReleaseBugfix();
        testProject.mvnReleaseBugfix();
        assertArtifactInLocalRepo(GROUP_ID, "independent-versions", expectedParentVersion + ".2");
        assertArtifactInLocalRepo(GROUP_ID, "core-utils", expectedCoreVersion + ".2");
        assertArtifactInLocalRepo(GROUP_ID, "console-app", expectedAppVersion + ".2");
    }

}
