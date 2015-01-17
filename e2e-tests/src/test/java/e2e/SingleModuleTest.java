package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.GitMatchers.hasTag;

public class SingleModuleTest {

    final String releaseVersion = String.valueOf(System.currentTimeMillis());
    final String expected = "1.0." + releaseVersion;
    final TestProject testProject = TestProject.singleModuleProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void canUpdateSnapshotVersionToReleaseVersionAndInstallToLocalRepo() throws Exception {
        assertThat(
            testProject.mvnRelease(releaseVersion),
            CoreMatchers.hasItem(containsString("Going to release single-module " + expected)));

        MvnRunner.assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects", "single-module", expected);
    }

    @Test
    public void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion() throws IOException, InterruptedException {
        testProject.mvnRelease(releaseVersion);
        String expectedTag = "single-module-" + expected;
        assertThat(testProject.local, hasTag(expectedTag));
        assertThat(testProject.origin, hasTag(expectedTag));
    }
}
