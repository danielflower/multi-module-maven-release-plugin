package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.IOException;

public class TestRunningTest {
    final TestProject projectWithTestsThatFail = TestProject.moduleWithTestFailure();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test(expected = MavenExecutionException.class)
    public void doesNotReleaseIfThereAreTestFailures() throws Exception {
        projectWithTestsThatFail.mvnRelease("1");
    }

    @Test
    public void ifTestsAreSkippedYouCanReleaseWithoutRunningThem() throws IOException {
        projectWithTestsThatFail.mvn(
            "-DreleaseVersion=1", "-DskipTests",
            "releaser:release");
    }

}
