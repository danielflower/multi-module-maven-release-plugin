package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.noneOf;
import static scaffolding.ExactCountMatcher.oneOf;

public class ExecutionTest {

    final TestProject testProject = TestProject.moduleWithProfilesProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void profilesNotPassedToTheReleaseExecutionAreNotPassedOnToTheDeployment() throws Exception {
        List<String> consoleOutput = testProject.mvnRelease("1");
        assertThat(consoleOutput, noneOf(containsString("The module-with-profiles test has run")));

        // can only uncomment if you know there are no globally activated profiles on the current computer
//        assertThat(consoleOutput, oneOf(containsString("[INFO] About to run mvn [install] with no profiles activated")));
    }

    @Test
    public void profilesPassedToTheReleaseExecutionArePassedOnToTheDeployment() throws Exception {
        List<String> consoleOutput = testProject.mvn("-DreleaseVersion=1", "releaser:release", "-P runTestsProfile");
        assertThat(consoleOutput, oneOf(containsString("The module-with-profiles test has run")));

        // can only uncomment if you know there are no globally activated profiles on the current computer
//        assertThat(consoleOutput, oneOf(containsString("[INFO] About to run mvn [install] with profiles [runTestsProfile]")));
    }

}
