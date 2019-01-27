package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.noneOf;
import static scaffolding.ExactCountMatcher.oneOf;

public class ExecutionTest {

    final TestProject testProject = TestProject.moduleWithProfilesProject();
    final String echoPluginOutput = "echo-maven-plugin running because profileActivatedByReleasePlugin is activated";

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void profilesNotPassedToTheReleaseExecutionAreNotPassedOnToTheDeploymentButConfiguredProfilesAre() throws Exception {
        List<String> consoleOutput = testProject.mvnRelease("1");
        assertThat(consoleOutput, noneOf(containsString("The module-with-profiles test has run")));
        assertThat(consoleOutput, oneOf(containsString(echoPluginOutput)));
    }

    @Test
    public void profilesPassedToTheReleaseExecutionArePassedOnToTheDeployment() throws Exception {
        List<String> consoleOutput = testProject.mvn("-DbuildNumber=1", "releaser:release", "-P runTestsProfile");
        assertThat(consoleOutput, oneOf(containsString("The module-with-profiles test has run")));
        assertThat(consoleOutput, oneOf(containsString(echoPluginOutput)));
    }

    @Test
    public void argumentsCanBePassed() throws Exception {
        List<String> consoleOutput = testProject.mvn("releaser:release", "-P runTestsProfile");
        assertThat(consoleOutput, oneOf(containsString("this is a system property: prop 1 value and prop2value")));
    }

    @Test
    public void envVarsAreInherited() throws Exception {
        List<String> consoleOutput = testProject.mvn("releaser:release", "-P runTestsProfile");
        assertThat(consoleOutput, oneOf(containsString("this is an env var: whatever")));
    }

    @Test
    public void mvn_optionsEnvVarIsPassedToExecution() throws Exception {
        testProject.setMvnOpts("-Dmvnopt=a-maven-option");
        List<String> consoleOutput = testProject.mvn("releaser:release", "-P runTestsProfile");
        assertThat(consoleOutput, oneOf(containsString("this is a property set via MAVEN_OPTS: a-maven-option")));
    }

    @Test
    public void userAndGlobalSettingsCanBeOverwrittenWithStandardMavenCommandLineParameters() throws Exception {
        File globalSettings = new File("test-projects/module-with-profiles/custom-settings.xml");
        List<String> consoleOutput = testProject.mvn("-DbuildNumber=1",
            "releaser:release", "-gs", globalSettings.getCanonicalPath());
        assertThat(consoleOutput, oneOf(containsString(echoPluginOutput)));
    }


}
