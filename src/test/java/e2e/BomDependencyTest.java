package e2e;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.GitMatchers.hasTag;

import java.util.List;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Test;

import scaffolding.MvnRunner;
import scaffolding.TestProject;

public class BomDependencyTest {

    final TestProject testProject = TestProject.dependencyManagementProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void willReleaseAllWhenBomIsChanged() throws Exception {
        testProject.mvnRelease("1");
        testProject.commitRandomFile("root-bom").pushIt();
        List<String> output = testProject.mvnRelease("2");

        assertTagExists("root-bom-1.0.2");
        assertTagExists("parent-module-1.2.3.2");
        assertTagExists("core-utils-2.0.2");
        assertTagExists("more-utils-10.0.2");
        assertTagExists("console-app-3.2.2");
        assertTagDoesNotExist("deep-dependencies-aggregator-1.0.2");
        
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.0.2 for root-bom as it has changed since the last release.")));
        assertThat(output, oneOf(containsString("[INFO] Releasing parent-module 1.2.3.2 as root-bom has changed.")));
        assertThat(output, oneOf(containsString("[INFO] Releasing more-utils 10.0.2 as root-bom has changed.")));
        assertThat(output, oneOf(containsString("[INFO] Releasing core-utils 2.0.2 as root-bom has changed.")));
        assertThat(output, oneOf(containsString("[INFO] Releasing console-app 3.2.2 as root-bom has changed.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.0.1 for dependencymanagement-aggregator as it has not been changed since that release.")));
    }

    @Test
    public void willReleaseAllButBomWhenParentIsChanged() throws Exception {
        testProject.mvnRelease("1");
        testProject.commitRandomFile("parent-module").pushIt();
        List<String> output = testProject.mvnRelease("2");

        assertTagDoesNotExist("root-bom-1.0.2");
        assertTagExists("parent-module-1.2.3.2");
        assertTagExists("core-utils-2.0.2");
        assertTagExists("more-utils-10.0.2");
        assertTagExists("console-app-3.2.2");
        assertTagDoesNotExist("deep-dependencies-aggregator-1.0.2");
        
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.0.1 for root-bom as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.2.3.2 for parent-module as it has changed since the last release.")));
        assertThat(output, oneOf(containsString("[INFO] Releasing more-utils 10.0.2 as parent-module has changed.")));
        assertThat(output, oneOf(containsString("[INFO] Releasing core-utils 2.0.2 as parent-module has changed.")));
        assertThat(output, oneOf(containsString("[INFO] Releasing console-app 3.2.2 as parent-module has changed.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.0.1 for dependencymanagement-aggregator as it has not been changed since that release.")));
    }

    @Test
    public void willReleaseConsoleAppAndCoreUtilsWhenCoreUtilsIsChanged() throws Exception {
        testProject.mvnRelease("1");
        testProject.commitRandomFile("the-core-utilities").pushIt();
        List<String> output = testProject.mvnRelease("2");

        assertTagDoesNotExist("root-bom-1.0.2");
        assertTagDoesNotExist("parent-module-1.2.3.2");
        assertTagExists("core-utils-2.0.2");
        assertTagDoesNotExist("more-utils-10.0.2");
        assertTagExists("console-app-3.2.2");
        assertTagDoesNotExist("deep-dependencies-aggregator-1.0.2");
        
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.0.1 for root-bom as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.2.3.1 for parent-module as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 10.0.1 for more-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 2.0.2 for core-utils as it has changed since the last release.")));
        assertThat(output, oneOf(containsString("[INFO] Releasing console-app 3.2.2 as core-utils has changed.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.0.1 for dependencymanagement-aggregator as it has not been changed since that release.")));
    }
    
    @Test
    public void willReleaseOnlyConsoleAppWhenConsoleAppIsChanged() throws Exception {
        testProject.mvnRelease("1");
        testProject.commitRandomFile("console-app").pushIt();
        List<String> output = testProject.mvnRelease("2");

        assertTagDoesNotExist("root-bom-1.0.2");
        assertTagDoesNotExist("parent-module-1.2.3.2");
        assertTagDoesNotExist("core-utils-2.0.2");
        assertTagDoesNotExist("more-utils-10.0.2");
        assertTagExists("console-app-3.2.2");
        assertTagDoesNotExist("deep-dependencies-aggregator-1.0.2");
        
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.0.1 for root-bom as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.2.3.1 for parent-module as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 10.0.1 for more-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 2.0.1 for core-utils as it has not been changed since that release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 3.2.2 for console-app as it has changed since the last release.")));
        assertThat(output, oneOf(containsString("[INFO] Will use version 1.0.1 for dependencymanagement-aggregator as it has not been changed since that release.")));
    }

    private void assertTagDoesNotExist(String tagName) {
        assertThat(testProject.local, not(hasTag(tagName)));
        assertThat(testProject.origin, not(hasTag(tagName)));
    }

    private void assertTagExists(String tagName) {
        assertThat(testProject.local, hasTag(tagName));
        assertThat(testProject.origin, hasTag(tagName));
    }
}
