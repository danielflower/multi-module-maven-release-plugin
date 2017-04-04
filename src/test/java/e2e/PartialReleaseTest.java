package e2e;

import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ExactCountMatcher.noneOf;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTagWithModuleVersion;
import static scaffolding.MvnRunner.assertArtifactInLocalRepo;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class PartialReleaseTest {

    public static final String      TEST_GROUP_ID             = "com.github.danielflower.mavenplugins.testprojects.parentassibling";
    final               String      expectedAggregatorVersion = "1.0";
    final               String      expectedParentVersion     = "1.0";
    final               String      expectedCoreVersion       = "2.0";
    final               String      expectedAppVersion        = "3.0";

    @Rule
    public TestProject testProject = new TestProject(ProjectType.PARENT_AS_SIBLING);

    @Test
    public void buildsAndInstallsAndTagsAllModules() throws Exception {
        List<String> commandOutput = testProject.mvnRelease("-DmodulesToRelease=core-utils");
        buildsEachProjectOnceAndOnlyOnce(commandOutput);
        installsAllModulesIntoTheRepoWithTheBuildNumber();
        theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion();
    }

    @Test
    public void whenNoChangesHaveBeenDetectedTheRequestedModuleIsBuiltAnyway() throws IOException,
                                                                                      InterruptedException {
        testProject.mvnRelease("-DmodulesToRelease=core-utils");
        testProject.mvnRelease("-DmodulesToRelease=core-utils");
    }

    private void buildsEachProjectOnceAndOnlyOnce(List<String> commandOutput) throws Exception {
        assertThat(commandOutput, allOf(oneOf(containsString("Going to release core-utils " + expectedCoreVersion)),
                                        oneOf(containsString("Building parent-as-sibling")),
                                        // once for initial build only
                                        oneOf(containsString("Building parent-module")),
                                        oneOf(containsString("Building core-utils")),
                                        noneOf(containsString("Building console-app")),
                                        oneOf(containsString("The Calculator Test has run"))));
    }

    private void installsAllModulesIntoTheRepoWithTheBuildNumber() throws Exception {
        assertArtifactInLocalRepo(TEST_GROUP_ID, "parent-module", expectedParentVersion);
        assertArtifactInLocalRepo(TEST_GROUP_ID, "core-utils", expectedCoreVersion);
    }

    private void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion() throws IOException,
                                                                                        InterruptedException {
        assertThat(testProject.local,
                   not(hasTagWithModuleVersion(TEST_GROUP_ID, "parent-as-sibling", expectedAggregatorVersion)));
        assertThat(testProject.origin,
                   not(hasTagWithModuleVersion(TEST_GROUP_ID, "parent-as-sibling", expectedAggregatorVersion)));
        assertThat(testProject.local, hasTagWithModuleVersion(TEST_GROUP_ID, "core-utils", expectedCoreVersion));
        assertThat(testProject.origin, hasTagWithModuleVersion(TEST_GROUP_ID, "core-utils", expectedCoreVersion));
        assertThat(testProject.local, not(hasTagWithModuleVersion(TEST_GROUP_ID, "console-app", expectedAppVersion)));
        assertThat(testProject.origin, not(hasTagWithModuleVersion(TEST_GROUP_ID, "console-app", expectedAppVersion)));
    }

    // TODO
    @Ignore("find better test")
    @Test
    public void thePomChangesAreRevertedAfterTheRelease() throws IOException, InterruptedException {
        ObjectId originHeadAtStart = head(testProject.origin);
        ObjectId localHeadAtStart = head(testProject.local);
        assertThat(originHeadAtStart, equalTo(localHeadAtStart));
        testProject.mvnRelease();
        assertThat(head(testProject.origin), equalTo(originHeadAtStart));
        assertThat(head(testProject.local), equalTo(localHeadAtStart));
        assertThat(testProject.local, hasCleanWorkingDirectory());
    }

    private ObjectId head(Git git) throws IOException {
        return git.getRepository().getRef("HEAD").getObjectId();
    }
}
