package e2e;

import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.CountMatcher.noneOf;
import static scaffolding.CountMatcher.oneOf;
import static scaffolding.GitMatchers.hasTag;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

public class NextMojoTest {

    @Rule
    public TestProject dependenciesProject = new TestProject(ProjectType.DEEP_DEPENDENCIES);

    @Test
    public void changesInTheRootAreDetected() throws Exception {
        TestProject simple = TestProject.project(ProjectType.SINGLE);
        simple.mvnRelease();
        simple.commitRandomFile(".");
        List<String> output = simple.mvnReleaserNext();
        assertThat(output,
                   oneOf(containsString("using 1.1 for single-module as it has changed since the " + "last release.")));
    }

    @Test
    public void doesNotReReleaseAModuleThatHasNotChanged() throws Exception {
        dependenciesProject.mvnRelease();

        dependenciesProject.commitRandomFile("console-app").pushIt();
        List<String> output = dependenciesProject.mvnReleaserNext();

        assertThat(output, oneOf(
            containsString("[INFO] using 1.0 for parent-module as it has not been changed since that release.")));
        assertThat(output, oneOf(
            containsString("[INFO] using 10.0 for more-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(
            containsString("[INFO] using 2.0 for core-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(
            containsString("[INFO] using 3.1 for console-app as it has changed since the last release.")));
        assertThat(output, oneOf(containsString(
            "[INFO] using 1.0 for deep-dependencies-aggregator as it has not been changed since that release.")));
    }

    @Test
    public void ifThereHaveBeenNoChangesThenReReleaseAllModules() throws Exception {
        List<String> firstBuildOutput = dependenciesProject.mvnRelease();
        assertThat(firstBuildOutput,
                   noneOf(containsString("No changes have been detected in any modules so will re-release them all")));
        List<String> output = dependenciesProject.mvnReleaserNext();

        assertThat(output, oneOf(containsString(
            "[INFO] using 1.1 for parent-module for rerelease.")));
        assertThat(output, oneOf(containsString(
            "[INFO] using 10.1 for more-utils for rerelease.")));
        assertThat(output, oneOf(containsString(
            "[INFO] using 2.1 for core-utils for rerelease.")));
        assertThat(output, oneOf(containsString(
            "[INFO] using 3.1 for console-app for rerelease.")));
        assertThat(output, oneOf(containsString(
            "[INFO] using 1.1 for deep-dependencies-aggregator for rerelease.")));
        assertThat(output, oneOf(
            containsString("[WARNING] No changes have been detected in any modules so will re-release them all")));
    }

    @Test
    public void ifThereHaveBeenNoChangesCanOptToReleaseNoModules() throws Exception {
        List<String> firstBuildOutput = dependenciesProject.mvnRelease();
        assertThat(firstBuildOutput,
                   noneOf(containsString("No changes have been detected in any modules so will re-release them all")));
        assertThat(firstBuildOutput,
                   noneOf(containsString("No changes have been detected in any modules so will not perform release")));
        List<String> output = dependenciesProject.mvnReleaserNext("-DnoChangesAction=ReleaseNone");

        assertThat(output, oneOf(containsString(
            "[INFO] using 1.0 for parent-module as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString(
            "[INFO] using 10.0 for more-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString(
            "[INFO] using 2.0 for core-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString(
            "[INFO] using 3.0 for console-app as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString(
            "[INFO] using 1.0 for deep-dependencies-aggregator as it has not been changed since that release.")));
        assertThat(output, oneOf(
            containsString("[WARNING] No changes have been detected in any modules so will not perform release")));
    }

    @Test
    public void ifADependencyHasNotChangedButSomethingItDependsOnHasChangedThenTheDependencyIsReReleased() throws
                                                                                                           Exception {
        dependenciesProject.mvnRelease();
        dependenciesProject.commitRandomFile("more-utilities").pushIt();
        List<String> output = dependenciesProject.mvnReleaserNext();

        assertTagDoesNotExist("console-app-3.2");
        assertTagDoesNotExist("parent-module-1.2");
        assertTagDoesNotExist("core-utils-2.2");
        assertTagDoesNotExist("more-utils-10.2");
        assertTagDoesNotExist("deep-dependencies-aggregator-1.2");

        assertThat(output, oneOf(containsString(
            "[INFO] using 1.0 for parent-module as it has not been changed since that release.")));
        assertThat(output, oneOf(
            containsString("[INFO] using 10.1 for more-utils as it has changed since the last release.")));
        assertThat(output, oneOf(containsString("[INFO] Releasing core-utils 2.1 as at least one dependency has changed.")));
        assertThat(output, oneOf(containsString("[INFO] Releasing console-app 3.1 as at least one dependency has changed.")));
        assertThat(output, oneOf(containsString(
            "[INFO] using 1.0 for deep-dependencies-aggregator as it has not been changed since that " +
                "release.")));
    }

    private void assertTagDoesNotExist(String tagName) {
        assertThat(dependenciesProject.local, not(hasTag(tagName)));
        assertThat(dependenciesProject.origin, not(hasTag(tagName)));
    }
}
