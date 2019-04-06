package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTag;
import static scaffolding.MvnRunner.assertArtifactInLocalRepo;

public class NestedModulesTest {

    final String expectedAggregatorVersion = "0.0.";
    final String expectedParentVersion = "1.2.3.";
    final String expectedCoreVersion = "2.0.";
    final String expectedAppVersion = "3.2.";
    final String expectedServerModulesVersion = "1.0.2.4.";
    final String expectedServerModuleAVersion = "3.0.";
    final String expectedServerModuleBVersion = "3.1.";
    final String expectedServerModuleCVersion = "3.2.";

    final TestProject testProject = TestProject.nestedProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void buildsAndInstallsAndTagsAllModules() throws Exception {
        buildsEachProjectOnceAndOnlyOnce(testProject.mvnRelease("1"));
        installsAllModulesIntoTheRepoWithTheBuildNumber();

        assertBothReposTagged("nested-project", expectedAggregatorVersion, "1");
        assertBothReposTagged("core-utils", expectedCoreVersion, "1");
        assertBothReposTagged("console-app", expectedAppVersion, "1");
        assertBothReposTagged("parent-module", expectedParentVersion, "1");
        assertBothReposTagged("server-modules", expectedServerModulesVersion, "1");
        assertBothReposTagged("server-module-a", expectedServerModuleAVersion, "1");
        assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "1");
        assertBothReposTagged("server-module-c", expectedServerModuleCVersion, "1");

        testProject.commitRandomFile("server-modules/server-module-b");
        testProject.mvn("releaser:release");

        assertBothReposNotTagged("nested-project", expectedAggregatorVersion, "2");
        assertBothReposNotTagged("core-utils", expectedCoreVersion, "2");
        assertBothReposTagged("console-app", expectedAppVersion, "2");
        assertBothReposNotTagged("parent-module", expectedParentVersion, "2");
        assertBothReposNotTagged("server-modules", expectedServerModulesVersion, "2");
        assertBothReposNotTagged("server-module-a", expectedServerModuleAVersion, "2");
        assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "2");
        assertBothReposTagged("server-module-c", expectedServerModuleCVersion, "2");

        testProject.commitRandomFile("parent-module");
        testProject.mvn("releaser:release");

        assertBothReposNotTagged("nested-project", expectedAggregatorVersion, "2");
        assertBothReposTagged("core-utils", expectedCoreVersion, "2");
        assertBothReposTagged("console-app", expectedAppVersion, "3");
        assertBothReposTagged("parent-module", expectedParentVersion, "2");
        assertBothReposNotTagged("server-modules", expectedServerModulesVersion, "3");
        assertBothReposTagged("server-module-a", expectedServerModuleAVersion, "2");
        assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "3");
        assertBothReposTagged("server-module-c", expectedServerModuleCVersion, "3");

        testProject.mvnRelease("4");
        assertBothReposTagged("nested-project", expectedAggregatorVersion, "4");
        assertBothReposTagged("core-utils", expectedCoreVersion, "4");
        assertBothReposTagged("console-app", expectedAppVersion, "4");
        assertBothReposTagged("parent-module", expectedParentVersion, "4");
        assertBothReposTagged("server-modules", expectedServerModulesVersion, "4");
        assertBothReposTagged("server-module-a", expectedServerModuleAVersion, "4");
        assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "4");
        assertBothReposTagged("server-module-c", expectedServerModuleCVersion, "4");

    }

    private void buildsEachProjectOnceAndOnlyOnce(List<String> commandOutput) throws Exception {
        assertThat(
            commandOutput,
            allOf(
                twoOf(containsString("Building nested-project")), // once for initial build; once for release build
                oneOf(containsString("Building core-utils")),
                oneOf(containsString("Building console-app")),
                oneOf(containsString("Building parent-module")),
                oneOf(containsString("Building server-modules")),
                oneOf(containsString("Building server-module-a")),
                oneOf(containsString("Building server-module-b")),
                oneOf(containsString("Building server-module-c"))
            )
        );
    }

    private void installsAllModulesIntoTheRepoWithTheBuildNumber() throws Exception {
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "nested-project", expectedAggregatorVersion + "1");
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "core-utils", expectedCoreVersion + "1");
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "console-app", expectedAppVersion + "1");
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "parent-module", expectedParentVersion + "1");
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "server-modules", expectedServerModulesVersion + "1");
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "server-module-a", expectedServerModuleAVersion + "1");
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "server-module-b", expectedServerModuleBVersion + "1");
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested.misnamed", "server-module-c", expectedServerModuleCVersion + "1");
    }

    private void assertBothReposTagged(String module, String version, String buildNumber) {
        String tag = module + "-" + version + buildNumber;
        assertThat(testProject.local, hasTag(tag));
        assertThat(testProject.origin, hasTag(tag));
    }

    private void assertBothReposNotTagged(String module, String version, String buildNumber) {
        String tag = module + "-" + version + buildNumber;
        assertThat(testProject.local, not(hasTag(tag)));
        assertThat(testProject.origin, not(hasTag(tag)));
    }

    @Test
    public void thePomChangesAreRevertedAfterTheRelease() throws IOException, InterruptedException {
        ObjectId originHeadAtStart = head(testProject.origin);
        ObjectId localHeadAtStart = head(testProject.local);
        assertThat(originHeadAtStart, equalTo(localHeadAtStart));
        testProject.mvnRelease("1");
        assertThat(head(testProject.origin), equalTo(originHeadAtStart));
        assertThat(head(testProject.local), equalTo(localHeadAtStart));
        assertThat(testProject.local, hasCleanWorkingDirectory());
    }

    private ObjectId head(Git git) throws IOException {
        return git.getRepository().getRefDatabase().findRef("HEAD").getObjectId();
    }
}
