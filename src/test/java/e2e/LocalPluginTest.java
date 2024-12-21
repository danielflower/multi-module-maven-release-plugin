package e2e;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;
import static scaffolding.GitMatchers.hasTag;

@Ignore
public class LocalPluginTest {

    final TestProject testProject = TestProject.localPluginProject();

    final String buildNumber = String.valueOf(System.currentTimeMillis());
    final String expected = "1.0." + buildNumber;

    @BeforeClass
    public static void installPluginToLocalRepo() {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void runWithLocalPluginSnapshotDependencyShouldSucceed() throws Exception {
        List<String> outputLines = testProject.mvn("releaser:release", "-DbuildNumber=" + buildNumber);

        for (String line : outputLines)
            System.out.println(line);

        //Validate released artifacts
        assertThat(testProject.local, hasTag("local-plugin-" + expected));
        assertThat(testProject.local, hasTag("local-maven-plugin-" + expected));
        assertThat(testProject.local, hasTag("simple-project-" + expected));

        //Validate plugin dependencies updated (once for plugin itself, once for pluginManagement)
        assertThat(
            outputLines,
            twoOf(containsString("Plugin dependency on local-maven-plugin rewritten to version " + expected)));
    }

    @Test
    public void runWithSnapshotPluginDependencyShouldFail() throws Exception {
        try {
            testProject.mvn("releaser:release", "-P snapshot-version");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, oneOf(containsString("[ERROR]  * simple-project references plugin commons-io 2.7-SNAPSHOT")));
        }
    }
}
