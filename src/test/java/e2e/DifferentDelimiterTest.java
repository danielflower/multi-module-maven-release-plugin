package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;

public class DifferentDelimiterTest {

    final String buildNumber = String.valueOf(System.currentTimeMillis());
    final String expected = "1.0.0-" + buildNumber;
    final TestProject testProject = TestProject.differentDelimiterProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void canUpdateSnapshotVersionToReleaseVersionAndInstallToLocalRepo() throws Exception {
        List<String> outputLines = testProject.mvnRelease(buildNumber);
        assertThat(outputLines, oneOf(containsString("Hello from version " + expected + "!")));

        MvnRunner.assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects", "different-delimiter", expected);

        assertThat(new File(testProject.localDir, "target/different-delimiter-" + expected + "-package.jar").exists(), is(true));
    }


}
