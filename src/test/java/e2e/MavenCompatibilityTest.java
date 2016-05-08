package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This test actually downloads multiple versions of maven and runs the plugin against them.
 */
public class MavenCompatibilityTest {

    final TestProject testProject = TestProject.singleModuleProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void maven_3_0_1() throws Exception {
        buildProjectWithMavenVersion("3.0.1");
    }

    @Test
    public void maven_3_0_4() throws Exception {
        buildProjectWithMavenVersion("3.0.4");
    }

    @Test
    public void maven_3_2_1() throws Exception {
        buildProjectWithMavenVersion("3.2.1");
    }

    @Test
    public void maven_3_3_9() throws Exception {
        buildProjectWithMavenVersion("3.3.9");
    }

    private void buildProjectWithMavenVersion(String mavenVersionToTest) throws IOException, InterruptedException, MavenInvocationException {
        String buildNumber = mavenVersionToTest.replace(".", "") + String.valueOf(System.currentTimeMillis());
        String expected = "1.0." + buildNumber;
        testProject.setMvnRunner(MvnRunner.mvn(mavenVersionToTest));
        testProject.mvnRelease(buildNumber);
        MvnRunner.assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects", "single-module", expected);
        assertThat(new File(testProject.localDir, "target/single-module-" + expected + "-package.jar").exists(), is(true));
    }

}
