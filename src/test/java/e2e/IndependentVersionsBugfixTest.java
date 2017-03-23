package e2e;

import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static scaffolding.MvnRunner.assertArtifactInLocalRepo;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class IndependentVersionsBugfixTest {

    private static final String GROUP_ID = "com.github.danielflower.mavenplugins.testprojects.independentversions";
    final String buildNumber = String.valueOf(System.currentTimeMillis());
    final String expectedParentVersion = "1." + buildNumber;
    final String expectedCoreVersion = "2." + buildNumber;
    final String expectedAppVersion = "3." + buildNumber;
    final TestProject testProject = TestProject.independentVersionsBugfixProject();
    private String branchName = "bugfix-" + buildNumber;

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Before
    public void releaseProject() throws Exception {
        testProject.mvnRelease(buildNumber);
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
