package e2e;

import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.GitMatchers.hasTag;

public class PluginDependencyTest {

    final TestProject testProject = TestProject.openapiSpecAsPluginDependency();


    @BeforeClass
    public static void installPluginToLocalRepo() {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void runWithPluginDependencyWithoutCommitInOpenApiSpecShouldSucceed() throws Exception {
        List<String> output1 = testProject.mvnRelease("1", "-X");
        assertThat(
                output1,
                allOf(
                        // Make sure that plugin uses the-openapi-spec 1.0.1
                        oneOf(containsString("Populating class realm plugin>org.openapitools:openapi-generator-maven-plugin")),
                        oneOf(containsString("Included: com.github.danielflower.mavenplugins.testprojects.openapi-spec-as-plugin-dependency:the-openapi-spec:jar:1.0.1"))
                )
        );
        testProject.commitRandomFile("the-openapi-spec").pushIt();
        List<String> output2 = testProject.mvnRelease("2", "-X");
        assertThat(
                output2,
                allOf(
                        // Make sure that plugin uses the-openapi-spec 1.0.2
                        oneOf(containsString("Populating class realm plugin>org.openapitools:openapi-generator-maven-plugin")),
                        oneOf(containsString("Included: com.github.danielflower.mavenplugins.testprojects.openapi-spec-as-plugin-dependency:the-openapi-spec:jar:1.0.2"))
                )
        );

        // The module in which a new file was committed
        assertTagExists("the-openapi-spec-1.0.1");
        assertTagExists("the-openapi-spec-1.0.2");

        // The module in which the module "the-openapi-spec" is used
        assertTagExists("the-service-impl-1.0.1");
        assertTagExists("the-service-impl-1.0.2");

        // The parent module
        assertTagExists("openapi-spec-as-plugin-dependency-aggregator-1.0.1");
        assertTagDoesNotExist("openapi-spec-as-plugin-dependency-aggregator-1.0.2");
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
