package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTag;
import static scaffolding.MvnRunner.assertArtifactInLocalRepo;

public class MultiModuleTest {

    public static final String[] ARTIFACT_IDS = new String[]{"inherited-versions-from-parent", "core-utils", "console-app"};
    final String releaseVersion = String.valueOf(System.currentTimeMillis());
    final String expected = "1.0." + releaseVersion;
    final TestProject testProject = TestProject.inheritedVersionsFromParent();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void buildsEachProjectOnceAndOnlyOnce() throws Exception {
        assertThat(
            testProject.mvnRelease(releaseVersion),
            allOf(
                oneOf(containsString("Going to release inherited-versions-from-parent " + expected)),
                twoOf(containsString("Building inherited-versions-from-parent")), // once for initial build; once for release build
                oneOf(containsString("Building core-utils")),
                oneOf(containsString("Building console-app")),
                oneOf(containsString("The Calculator Test has run"))
            )
        );
    }

    @Test
    public void installsAllModulesIntoTheRepoWithTheReleaseVersion() throws Exception {
        testProject.mvnRelease(releaseVersion);
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.versioninheritor", "inherited-versions-from-parent", expected);
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.versioninheritor", "core-utils", expected);
        assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.versioninheritor", "console-app", expected);
    }

    @Test
    public void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion() throws IOException, InterruptedException {
        testProject.mvnRelease(releaseVersion);
        for (String artifactId : ARTIFACT_IDS) {
            String expectedTag = artifactId + "-" + expected;
            assertThat(testProject.local, hasTag(expectedTag));
            assertThat(testProject.origin, hasTag(expectedTag));
        }
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

    @Test
    public void whenOneModuleDependsOnAnotherThenWhenReleasingThisDependencyHasTheRelaseVersion() {
        // TODO: implement this
    }

    private ObjectId head(Git git) throws IOException {
        return git.getRepository().getRef("HEAD").getObjectId();
    }
}
