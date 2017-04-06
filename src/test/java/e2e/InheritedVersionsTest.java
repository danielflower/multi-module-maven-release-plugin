package e2e;

import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.CountMatcher.oneOf;
import static scaffolding.CountMatcher.twoOf;
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

import de.hilling.maven.release.TestUtils;

public class InheritedVersionsTest {

    private static final String[] ARTIFACT_IDS = new String[]{"inherited-versions-from-parent", "core-utils",
                                                              "console-app"};
    private static final String   GROUP_ID     = TestUtils.TEST_GROUP_ID + ".versioninheritor";
    private final        String   expected     = "1.0";

    @Rule
    public TestProject testProject = new TestProject(ProjectType.INHERITED_VERSIONS);

    @Test
    public void buildsAndInstallsAndTagsAllModules() throws Exception {
        buildsEachProjectOnceAndOnlyOnce(testProject.mvnRelease());
        installsAllModulesIntoTheRepoWithTheBuildNumber();
        theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion();
    }

    private void buildsEachProjectOnceAndOnlyOnce(List<String> commandOutput) throws Exception {
        assertThat(commandOutput,
                   allOf(oneOf(containsString("Going to release inherited-versions-from-parent " + expected)),
                         twoOf(containsString("Building inherited-versions-from-parent")),
                         // once for initial build; once for release build
                         oneOf(containsString("Building core-utils")), oneOf(containsString("Building console-app")),
                         oneOf(containsString("The Calculator Test has run"))));
    }

    private void installsAllModulesIntoTheRepoWithTheBuildNumber() throws Exception {
        assertArtifactInLocalRepo(GROUP_ID, "inherited-versions-from-parent", expected);
        assertArtifactInLocalRepo(GROUP_ID, "core-utils", expected);
        assertArtifactInLocalRepo(GROUP_ID, "console-app", expected);
    }

    private void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion() throws IOException,
                                                                                        InterruptedException {
        for (String artifactId : ARTIFACT_IDS) {
            assertThat(testProject.local, hasTagWithModuleVersion(GROUP_ID, artifactId, expected));
            assertThat(testProject.origin, hasTagWithModuleVersion(GROUP_ID, artifactId, expected));
        }
    }

    // TODO fix this globally
    @Ignore
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

    //    @Test
    //    public void whenOneModuleDependsOnAnotherThenWhenReleasingThisDependencyHasTheRelaseVersion() {
    //        // TODO: implement this
    //    }

    private ObjectId head(Git git) throws IOException {
        return git.getRepository().getRef("HEAD").getObjectId();
    }
}
