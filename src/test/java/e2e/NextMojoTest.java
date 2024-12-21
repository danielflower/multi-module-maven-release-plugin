package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.noneOf;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.GitMatchers.hasTag;

public class NextMojoTest {

    final TestProject testProject = TestProject.deepDependenciesProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void changesInTheRootAreDetected() throws Exception {
        TestProject simple = TestProject.singleModuleProject();
        simple.mvnRelease("1");
        simple.commitRandomFile(".");
        List<String> output = simple.mvnReleaserNext("2");
        assertThat(output, oneOf(containsString("Will use version 1.0.2 for single-module as it has changed since the last release.")));
    }

    @Test
    public void doesNotReReleaseAModuleThatHasNotChanged() throws Exception {
        testProject.mvnRelease("1");

        testProject.commitRandomFile("console-app").pushIt();
        List<String> output = testProject.mvnReleaserNext("2");
        assertTagDoesNotExist("console-app-3.2.2");
        assertTagDoesNotExist("parent-module-1.2.3.2");
        assertTagDoesNotExist("core-utils-2.0.2");
        assertTagDoesNotExist("more-utils-10.0.2");
        assertTagDoesNotExist("deep-dependencies-aggregator-1.0.2");

        assertThat(output, oneOf(containsString("[INFO] Will use version 1.2.3.1 for parent-module as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 10.0.1 for more-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 2.0.1 for core-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 3.2.2 for console-app as it has changed since the last release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.0.1 for deep-dependencies-aggregator as it has not been changed since that release.")));
    }

    @Test
    public void ifThereHaveBeenNoChangesThenReReleaseAllModules() throws Exception {
        List<String> firstBuildOutput = testProject.mvnRelease("1");
        assertThat(firstBuildOutput, noneOf(containsString("No changes have been detected in any modules so will re-release them all")));
        List<String> output = testProject.mvnReleaserNext("2");

        assertTagDoesNotExist("console-app-3.2.2");
        assertTagDoesNotExist("parent-module-1.2.3.2");
        assertTagDoesNotExist("core-utils-2.0.2");
        assertTagDoesNotExist("more-utils-10.0.2");
        assertTagDoesNotExist("deep-dependencies-aggregator-1.0.2");

        assertThat(output, oneOf(containsString("[INFO] Will use version 1.2.3.1 for parent-module as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 10.0.1 for more-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 2.0.1 for core-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 3.2.1 for console-app as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.0.1 for deep-dependencies-aggregator as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[WARNING] No changes have been detected in any modules so will re-release them all")));
    }

    @Test
    public void ifThereHaveBeenNoChangesCanOptToReleaseNoModules() throws Exception {
        List<String> firstBuildOutput = testProject.mvnRelease("1");
        assertThat(firstBuildOutput, noneOf(containsString("No changes have been detected in any modules so will re-release them all")));
        assertThat(firstBuildOutput, noneOf(containsString("No changes have been detected in any modules so will not perform release")));
        List<String> output = testProject.mvnReleaserNext("2", "-DnoChangesAction=ReleaseNone");

        assertTagDoesNotExist("console-app-3.2.2");
        assertTagDoesNotExist("parent-module-1.2.3.2");
        assertTagDoesNotExist("core-utils-2.0.2");
        assertTagDoesNotExist("more-utils-10.0.2");
        assertTagDoesNotExist("deep-dependencies-aggregator-1.0.2");

        assertThat(output, oneOf(containsString("[INFO] Will use version 1.2.3.1 for parent-module as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 10.0.1 for more-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 2.0.1 for core-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 3.2.1 for console-app as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.0.1 for deep-dependencies-aggregator as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[WARNING] No changes have been detected in any modules so will not perform release")));
    }

    @Test
    public void ifADependencyHasNotChangedButSomethingItDependsOnHasChangedThenTheDependencyIsReReleased() throws Exception {
        testProject.mvnRelease("1");
        testProject.commitRandomFile("more-utilities").pushIt();
        List<String> output = testProject.mvnReleaserNext("2");

        assertTagDoesNotExist("console-app-3.2.2");
        assertTagDoesNotExist("parent-module-1.2.3.2");
        assertTagDoesNotExist("core-utils-2.0.2");
        assertTagDoesNotExist("more-utils-10.0.2");
        assertTagDoesNotExist("deep-dependencies-aggregator-1.0.2");

        assertThat(output, oneOf(containsString("[INFO] Will use version 1.2.3.1 for parent-module as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 10.0.2 for more-utils as it has changed since the last release.")));
        assertThat(output, oneOf(containsString("[INFO] Releasing core-utils 2.0.2 as more-utils has changed.")));
        assertThat(output, oneOf(containsString("[INFO] Releasing console-app 3.2.2 as core-utils has changed.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.0.1 for deep-dependencies-aggregator as it has not been changed since that release.")));
    }

    private void assertTagDoesNotExist(String tagName) {
        assertThat(testProject.local, not(hasTag(tagName)));
        assertThat(testProject.origin, not(hasTag(tagName)));
    }

}
