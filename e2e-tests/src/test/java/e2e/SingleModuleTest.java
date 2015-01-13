package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.MvnRunner.mvn;

public class SingleModuleTest {

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void canUpdateSnapshotVersionToReleaseVersionAndInstallToLocalRepo() throws Exception {
        String releaseVersion = String.valueOf(System.currentTimeMillis());
        String expected = "1.0." + releaseVersion;

        assertThat(
                mvn("-DreleaseVersion=" + releaseVersion, "multi-module-release:release"),
                hasItem(containsString("Going to release test-project-single-module " + expected)));

        MvnRunner.assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects", MvnRunner.test_project_single_module, expected);
    }
}
