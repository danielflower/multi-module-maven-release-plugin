package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.Photocopier.copyTestProjectToTemporaryLocation;

public class SingleModuleTest {

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void canDoIt() throws IOException, InterruptedException {
        File projectDir = copyTestProjectToTemporaryLocation("test-project-single-module");
        String releaseVersion = String.valueOf(System.currentTimeMillis());
        List<String> output = MvnRunner.runReleaseOn(projectDir, releaseVersion);
        assertThat(output, hasItem(containsString("1.0-SNAPSHOT")));
    }

}
