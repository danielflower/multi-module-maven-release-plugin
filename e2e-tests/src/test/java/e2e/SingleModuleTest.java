package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class SingleModuleTest {

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void canUpdateSnapshotVersionToReleaseVersionAndInstallToLocalRepo() throws Exception {
        String releaseVersion = String.valueOf(System.currentTimeMillis());
        String expected = "1.0." + releaseVersion;
        TestProject testProject = TestProject.singleModuleProject();


        assertThat(
            testProject.mvnRelease(releaseVersion),
            CoreMatchers.hasItem(containsString("Going to release single-module " + expected)));

        MvnRunner.assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects", "single-module", expected);
    }
}
