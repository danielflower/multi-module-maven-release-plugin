package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.Photocopier.copyTestProjectToTemporaryLocation;

public class HelpTest {

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void runningTheHelpMojoTellsYouAboutThePluging() throws Exception {
        File projectDir = copyTestProjectToTemporaryLocation("test-project-single-module");
        List<String> output = MvnRunner.runMaven(projectDir, "multi-module-release:help");
        assertThat(output, allOf(
            hasItem(containsString("This plugin has 2 goals:"))
        ));

    }
}
