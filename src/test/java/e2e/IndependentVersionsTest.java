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

public class IndependentVersionsTest {

    final String releaseVersion = String.valueOf(System.currentTimeMillis());
    final String expectedParentVersion = "1.0." + releaseVersion;
    final String expectedCoreVersion = "2.0." + releaseVersion;
    final String expectedAppVersion = "3.2." + releaseVersion;
    final TestProject testProject = TestProject.independentVersionsProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void buildsAndInstallsAndTagsAllModules() throws Exception {
        buildsEachProjectOnceAndOnlyOnce(testProject.mvnRelease(releaseVersion));
        installsAllModulesIntoTheRepoWithTheReleaseVersion();
        theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion();
    }

    private void buildsEachProjectOnceAndOnlyOnce(List<String> commandOutput) throws Exception {
        assertThat(
            commandOutput,
            allOf(
                oneOf(containsString("Going to release independent-versions " + expectedParentVersion)),
                twoOf(containsString("Building independent-versions")), // once for initial build; once for release build
                oneOf(containsString("Building core-utils")),
                oneOf(containsString("Building console-app")),
                oneOf(containsString("The Calculator Test has run"))
            )
        );
    }

    private void installsAllModulesIntoTheRepoWithTheReleaseVersion() throws Exception {
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.independentversions", "independent-versions", expectedParentVersion);
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.independentversions", "core-utils", expectedCoreVersion);
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.independentversions", "console-app", expectedAppVersion);
    }

    private void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion() throws IOException, InterruptedException {
        assertThat(testProject.local, hasTag("independent-versions-" + expectedParentVersion));
        assertThat(testProject.origin, hasTag("independent-versions-" + expectedParentVersion));

        assertThat(testProject.local, hasTag("core-utils-" + expectedCoreVersion));
        assertThat(testProject.origin, hasTag("core-utils-" + expectedCoreVersion));

        assertThat(testProject.local, hasTag("console-app-" + expectedAppVersion));
        assertThat(testProject.origin, hasTag("console-app-" + expectedAppVersion));
    }

    @Test
    public void thePomChangesAreRevertedAfterTheRelease() throws IOException, InterruptedException {
        ObjectId originHeadAtStart = head(testProject.origin);
        ObjectId localHeadAtStart = head(testProject.local);
        assertThat(originHeadAtStart, equalTo(localHeadAtStart));
        testProject.mvnRelease(releaseVersion);
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
