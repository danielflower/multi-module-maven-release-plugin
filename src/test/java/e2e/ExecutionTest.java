package e2e;

import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.noneOf;
import static scaffolding.ExactCountMatcher.oneOf;

import java.io.File;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.TestUtils;

public class ExecutionTest {

    private static final String ECHO_PLUGIN_OUTPUT = "echo-maven-plugin running because profileActivatedByReleasePlugin is activated";

    @Rule
    public TestProject testProject = new TestProject(ProjectType.MODULE_PROFILES);

    @Test
    public void profilesNotPassedToTheReleaseExecutionAreNotPassedOnToTheDeploymentButConfiguredProfilesAre() throws
                                                                                                              Exception {
        List<String> consoleOutput = testProject.mvnRelease();
        assertThat(consoleOutput, noneOf(containsString("The module-with-profiles test has run")));
        assertThat(consoleOutput, oneOf(containsString(ECHO_PLUGIN_OUTPUT)));
    }

    @Test
    public void profilesPassedToTheReleaseExecutionArePassedOnToTheDeployment() throws Exception {
        List<String> consoleOutput = testProject.mvn(TestUtils.RELEASE_GOAL, "-PrunTestsProfile");
        assertThat(consoleOutput, oneOf(containsString("The module-with-profiles test has run")));
        assertThat(consoleOutput, oneOf(containsString(ECHO_PLUGIN_OUTPUT)));
    }

    @Test
    public void userAndGlobalSettingsCanBeOverwrittenWithStandardMavenCommandLineParameters() throws Exception {
        File globalSettings = new File("test-projects/module-with-profiles/custom-settings.xml");
        List<String> consoleOutput = testProject.mvn(TestUtils.RELEASE_GOAL, "-gs", globalSettings.getCanonicalPath());
        assertThat(consoleOutput, oneOf(containsString(ECHO_PLUGIN_OUTPUT)));
    }
}
