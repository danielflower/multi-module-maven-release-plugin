package e2e;

import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTagWithModuleVersion;
import static scaffolding.MvnRunner.assertArtifactInLocalRepo;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.releaseinfo.ReleaseInfoStorage;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableQualifiedArtifact;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;

public class IndependentVersionsTest {

    private static final String INDEPENDENT_VERSIONS_GROUPID = "com.github.danielflower.mavenplugins.testprojects.independentversions";
    private final String      expectedParentVersion = "1.0";
    private final String      expectedCoreVersion   = "2.0";
    private final String      expectedAppVersion    = "3.0";

    @Rule
    public TestProject testProject = new TestProject(ProjectType.INDEPENDENT_VERSIONS);
    private ImmutableQualifiedArtifact.Builder builder;

    @Test
    public void buildsAndInstallsAndTagsAllModules() throws Exception {
        buildsEachProjectOnceAndOnlyOnce(testProject.mvnRelease());
        installsAllModulesIntoTheRepoWithTheBuildNumber();
        theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion();
    }

    @Test
    public void referCorrectTagIfModuleNotReleased() throws Exception {
        testProject.mvnRelease();
        final ReleaseInfoStorage releaseInfoStorage = new ReleaseInfoStorage(testProject.localDir, testProject.local);
        final ReleaseInfo beforeRelease = releaseInfoStorage.load();
        testProject.commitRandomFile("console-app");
        testProject.mvnRelease();
        final ReleaseInfo afterRelease = releaseInfoStorage.load();
        builder = ImmutableQualifiedArtifact.builder().groupId(
            INDEPENDENT_VERSIONS_GROUPID);
        final ImmutableQualifiedArtifact consoleApp = builder.artifactId("console-app").build();
        assertThat(afterRelease.versionForArtifact(consoleApp).get().getReleaseTag(), not(equalTo(beforeRelease.versionForArtifact
                                                                                              (consoleApp).get().getReleaseTag())));
        final ImmutableQualifiedArtifact coreUtils = builder.artifactId("core-utils").build();
        assertThat(afterRelease.versionForArtifact(coreUtils).get(), equalTo(beforeRelease.versionForArtifact
                                                                                              (coreUtils).get()));
    }

    private void buildsEachProjectOnceAndOnlyOnce(List<String> commandOutput) throws Exception {
        assertThat(commandOutput,
                   allOf(oneOf(containsString("Going to release independent-versions " + expectedParentVersion)),
                         twoOf(containsString("Building independent-versions")),
                         // once for initial build; once for release build
                         oneOf(containsString("Building core-utils")), oneOf(containsString("Building console-app")),
                         oneOf(containsString("The Calculator Test has run"))));
    }

    private void installsAllModulesIntoTheRepoWithTheBuildNumber() throws Exception {
        assertArtifactInLocalRepo(INDEPENDENT_VERSIONS_GROUPID,
                                  "independent-versions", expectedParentVersion);
        assertArtifactInLocalRepo(INDEPENDENT_VERSIONS_GROUPID, "core-utils",
                                  expectedCoreVersion);
        assertArtifactInLocalRepo(INDEPENDENT_VERSIONS_GROUPID,
                                  "console-app", expectedAppVersion);
    }

    private void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion() throws IOException,
                                                                                        InterruptedException {
        assertThat(testProject.local, hasTagWithModuleVersion(INDEPENDENT_VERSIONS_GROUPID, "independent-versions", expectedParentVersion));
        assertThat(testProject.local, hasTagWithModuleVersion(INDEPENDENT_VERSIONS_GROUPID, "core-utils", expectedCoreVersion));
        assertThat(testProject.local, hasTagWithModuleVersion(INDEPENDENT_VERSIONS_GROUPID, "console-app", expectedAppVersion));

        assertThat(testProject.origin, hasTagWithModuleVersion(INDEPENDENT_VERSIONS_GROUPID, "independent-versions", expectedParentVersion));
        assertThat(testProject.origin, hasTagWithModuleVersion(INDEPENDENT_VERSIONS_GROUPID, "core-utils", expectedCoreVersion));
        assertThat(testProject.origin, hasTagWithModuleVersion(INDEPENDENT_VERSIONS_GROUPID, "console-app", expectedAppVersion));
    }

    // TODO
    @Ignore("update test")
    @Test
    public void thePomChangesAreRevertedAfterTheRelease() throws IOException, InterruptedException {
        ObjectId originHeadAtStart = head(testProject.origin);
        ObjectId localHeadAtStart = head(testProject.local);
        assertThat(originHeadAtStart, equalTo(localHeadAtStart));
        testProject.mvnRelease();
        assertThat(testProject.local, hasCleanWorkingDirectory());
        assertThat(head(testProject.origin), equalTo(originHeadAtStart));
        assertThat(head(testProject.local), equalTo(localHeadAtStart));
    }

    @Test
    public void whenRunFromASubFolderItShowsAnError() throws IOException, InterruptedException {
        try {
            new MvnRunner().runMaven(new File(testProject.localDir, "console-app"), "releaser:release");
            Assert.fail("Should not have worked");
        } catch (MavenExecutionException e) {
            assertThat(e.output, twoOf(
                containsString("The release plugin can only be run from the root folder of your Git repository")));
            assertThat(e.output, oneOf(
                containsString("Try running the release plugin from " + testProject.localDir.getCanonicalPath())));
        }
    }

    private ObjectId head(Git git) throws IOException {
        return git.getRepository().getRef("HEAD").getObjectId();
    }
}
