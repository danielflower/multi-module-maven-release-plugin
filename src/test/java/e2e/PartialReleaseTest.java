package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ExactCountMatcher.noneOf;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTag;
import static scaffolding.MvnRunner.assertArtifactInLocalRepo;

public class PartialReleaseTest {

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
        List<String> commandOutput = testProject.mvnRelease(buildNumber, "-DmodulesToRelease=core-utils");
        buildsEachProjectOnceAndOnlyOnce(commandOutput);
        installsAllModulesIntoTheRepoWithTheBuildNumber();
        theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion();
    }

    @Test
    public void whenNoChangesHaveBeenDetectedTheRequestedModuleIsBuiltAnyway() throws IOException, InterruptedException {
        testProject.mvnRelease("1", "-DmodulesToRelease=core-utils");
        testProject.mvnRelease(buildNumber, "-DmodulesToRelease=core-utils");
    }

    private void buildsEachProjectOnceAndOnlyOnce(List<String> commandOutput) throws Exception {
        assertThat(
            commandOutput,
            allOf(
                oneOf(containsString("Going to release core-utils " + expectedCoreVersion)),
                oneOf(containsString("Building parent-as-sibling")), // once for initial build only
                oneOf(containsString("Building parent-module")),
                oneOf(containsString("Building core-utils")),
                noneOf(containsString("Building console-app")),
                oneOf(containsString("The Calculator Test has run"))
            )
        );
    }

    private void installsAllModulesIntoTheRepoWithTheBuildNumber() throws Exception {
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.parentassibling", "parent-module", expectedParentVersion);
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.parentassibling", "core-utils", expectedCoreVersion);
    }

    private void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion() throws IOException, InterruptedException {
        assertThat(testProject.local, not(hasTag("parent-as-sibling-" + expectedAggregatorVersion)));
        assertThat(testProject.origin, not(hasTag("parent-as-sibling-" + expectedAggregatorVersion)));
        // The parent module should have a tag but this is non-trivial to implement in a maintainable manner.
        // The problem is that the plugin executes Maven but does not know which builds Maven will actually
        // run. I have considered 3 strategies:
        // 1. Parse the console output from Maven. I worry this will result in future breakages if Maven output changes.
        // 2. Figure out which modules Maven will build. There are Maven libraries that make this possible however it again
        //    feels like something that could break depending on the maven version.
        // 3. Get the plugin to work with --projects flag. This is probably the best way and almost works, however the Pom
        //    updater only updates the modules that are being built, which sounds okay but it turns out that it results
        //    in modules referencing a snapshot parent that doesn't exist, and maven checks this even though the project
        //    with the bad reference isn't being checked.
        // Although there are solutions to the problem, they all felt too brittle.
//        assertThat(testProject.local, hasTag("parent-module-" + expectedParentVersion));
//        assertThat(testProject.origin, hasTag("parent-module-" + expectedParentVersion));
        assertThat(testProject.local, hasTag("core-utils-" + expectedCoreVersion));
        assertThat(testProject.origin, hasTag("core-utils-" + expectedCoreVersion));
        assertThat(testProject.local, not(hasTag("console-app-" + expectedAppVersion)));
        assertThat(testProject.origin, not(hasTag("console-app-" + expectedAppVersion)));
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

    private ObjectId head(Git git) throws IOException {
        return git.getRepository().getRefDatabase().findRef("HEAD").getObjectId();
    }

}
