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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTag;
import static scaffolding.MvnRunner.assertArtifactInLocalRepo;

public class ParentAsSiblingTest {

    final String buildNumber = String.valueOf(System.currentTimeMillis());
    final String expectedAggregatorVersion = "1.0." + buildNumber;
    final String expectedParentVersion = "1.2.3." + buildNumber;
    final String expectedCoreVersion = "2.0." + buildNumber;
    final String expectedAppVersion = "3.2." + buildNumber;
    final TestProject testProject = TestProject.parentAsSibilngProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void buildsAndInstallsAndTagsAllModules() throws Exception {
        buildsEachProjectOnceAndOnlyOnce(testProject.mvnRelease(buildNumber));
        installsAllModulesIntoTheRepoWithTheBuildNumber();
        theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion();
    }

    private void buildsEachProjectOnceAndOnlyOnce(List<String> commandOutput) throws Exception {
        assertThat(
            commandOutput,
            allOf(
                oneOf(containsString("Going to release parent-as-sibling " + expectedAggregatorVersion)),
                twoOf(containsString("Building parent-as-sibling")), // once for initial build; once for release build
                oneOf(containsString("Building parent-module")),
                oneOf(containsString("Building core-utils")),
                oneOf(containsString("Building console-app")),
                oneOf(containsString("The Calculator Test has run"))
            )
        );
    }

    private void installsAllModulesIntoTheRepoWithTheBuildNumber() throws Exception {
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.independentversions", "parent-as-sibling", expectedAggregatorVersion);
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.independentversions", "parent-module", expectedParentVersion);
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.independentversions", "core-utils", expectedCoreVersion);
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.independentversions", "console-app", expectedAppVersion);
    }

    private void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion() throws IOException, InterruptedException {
        assertTagExists("parent-as-sibling-" + expectedAggregatorVersion);
        assertTagExists("parent-module-" + expectedParentVersion);
        assertTagExists("core-utils-" + expectedCoreVersion);
        assertTagExists("console-app-" + expectedAppVersion);
    }

    private void assertTagExists(String tagName) {
        assertThat(testProject.local, hasTag(tagName));
        assertThat(testProject.origin, hasTag(tagName));
    }

    @Test
    public void thePomChangesAreRevertedAfterTheRelease() throws IOException, InterruptedException {
        ObjectId originHeadAtStart = head(testProject.origin);
        ObjectId localHeadAtStart = head(testProject.local);
        assertThat(originHeadAtStart, equalTo(localHeadAtStart));
        testProject.mvnRelease(buildNumber);
        assertThat(head(testProject.origin), equalTo(originHeadAtStart));
        assertThat(head(testProject.local), equalTo(localHeadAtStart));
        assertThat(testProject.local, hasCleanWorkingDirectory());
    }

//    @Test
//    public void whenOneModuleDependsOnAnotherThenWhenReleasingThisDependencyHasTheRelaseVersion() {
//        // TODO: implement this
//    }

    private ObjectId head(Git git) throws IOException {
        return git.getRepository().getRef("HEAD").getObjectId();
    }
}
