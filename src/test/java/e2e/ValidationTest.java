package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;

public class ValidationTest {

    final TestProject testProject = TestProject.singleModuleProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void ifTheSameVersionIsReleasedTwiceItErrorsLoudly() throws Exception {
        testProject.mvnRelease("1");
        try {
            testProject.mvnRelease("1");
            Assert.fail("Should not have completed running");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output,
                twoOf(containsString("There is already a tag named single-module-1.0.1 in this repository.")));
            assertThat(mee.output,
                oneOf(containsString("It is likely that this version has been released before.")));
            assertThat(mee.output,
                oneOf(containsString("Please try incrementing the build number and trying again.")));
        }
    }

    @Test
    public void failsIfThereAreUntrackedFiles() throws IOException, InterruptedException {
        new File(testProject.localDir, "untracked.txt").createNewFile();
        try {
            testProject.mvnRelease("1");
            Assert.fail("Should not have worked the second time");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output,
                twoOf(containsString("Cannot release with uncommitted changes")));
        }
    }

    @Test
    public void failsIfThereAreUncommittedFiles() throws IOException, InterruptedException, GitAPIException {
        new File(testProject.localDir, "uncommitted.txt").createNewFile();
        testProject.local.add().addFilepattern("uncommitted.txt").call();
        try {
            testProject.mvnRelease("1");
            Assert.fail("Should not have worked the second time");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output,
                twoOf(containsString("Cannot release with uncommitted changes")));
        }
    }

}
