package e2e;

import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.GitMatchers.hasTag;

public class LocalPluginTest {

    final TestProject testProject = TestProject.localPluginProject();

    @BeforeClass
    public static void installPluginToLocalRepo() {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void runWithLocalPluginSnapshotDependencyShouldSucceed() throws Exception {
        List<String> outputLines = testProject.mvn("releaser:release", "-P project-version");

        for (String line : outputLines)
            System.out.println(line);

        assertThat(testProject.local, hasTag("local-plugin-1.0.0"));
        assertThat(testProject.local, hasTag("local-maven-plugin-1.0.0"));
        assertThat(testProject.local, hasTag("simple-project-1.0.0"));
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
