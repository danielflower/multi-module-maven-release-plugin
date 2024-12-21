package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;

public class TestRunningTest {
    final TestProject projectWithTestsThatFail = TestProject.moduleWithTestFailure();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void doesNotReleaseIfThereAreTestFailuresButTagsAreStillWritten() throws Exception {
        try {
            projectWithTestsThatFail.mvnRelease("1");
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {

        }
        assertThat(projectWithTestsThatFail.local, hasCleanWorkingDirectory());
        assertThat(projectWithTestsThatFail.local.tagList().call().get(0).getName(), is("refs/tags/module-with-test-failure-1.0.1"));
        assertThat(projectWithTestsThatFail.origin.tagList().call().get(0).getName(), is("refs/tags/module-with-test-failure-1.0.1"));
    }

    @Test
    public void ifTestsAreSkippedYouCanReleaseWithoutRunningThem() throws IOException {
        projectWithTestsThatFail.mvn(
            "-DbuildNumber=1", "-DskipTests",
            "releaser:release");
    }

}
