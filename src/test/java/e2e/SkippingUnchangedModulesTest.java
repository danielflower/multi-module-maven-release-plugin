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

public class SkippingUnchangedModulesTest {

    final TestProject testProject = TestProject.parentAsSibilngProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void doesNotReReleaseAModuleThatHasNotChanged() throws Exception {
        testProject.mvnRelease("1");
        assertTagExists("parent-as-sibling-1.0.1");
        assertTagExists("parent-module-1.2.3.1");
        assertTagExists("core-utils-2.0.1");
        assertTagExists("console-app-3.2.1");

        testProject.commitRandomFile("console-app").pushIt();
        List<String> output = testProject.mvnRelease("2");
        assertTagExists("console-app-3.2.2");
        assertTagDoesNotExist("parent-module-1.2.3.2");
        assertTagDoesNotExist("core-utils-2.0.2");
        assertTagDoesNotExist("parent-as-sibling-1.0.2");

        assertThat(output, oneOf(containsString("Going to release console-app 3.2.2")));
        assertThat(output, noneOf(containsString("Going to release parent-module")));
        assertThat(output, noneOf(containsString("Going to release core-utils")));
        assertThat(output, noneOf(containsString("Going to release parent-as-sibling")));
    }

    private void assertTagExists(String tagName) {
        assertThat(testProject.local, hasTag(tagName));
        assertThat(testProject.origin, hasTag(tagName));
    }

    private void assertTagDoesNotExist(String tagName) {
        assertThat(testProject.local, not(hasTag(tagName)));
        assertThat(testProject.origin, not(hasTag(tagName)));
    }


}
