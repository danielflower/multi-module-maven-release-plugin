package e2e;

import com.github.danielflower.mavenplugins.release.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.GitMatchers;
import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.File;
import java.io.IOException;

import static com.github.danielflower.mavenplugins.release.FileUtils.pathOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;

public class ValidationTest {

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void ifTheSameVersionIsReleasedTwiceItErrorsLoudly() throws Exception {
        TestProject testProject = TestProject.singleModuleProject();
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
        TestProject testProject = TestProject.singleModuleProject();
        new File(testProject.localDir, "untracked.txt").createNewFile();
        new File(testProject.localDir, "someFolder").mkdir();
        new File(testProject.localDir, "someFolder/anotherUntracked.txt").createNewFile();
        try {
            testProject.mvnRelease("1");
            Assert.fail("Should not have worked the second time");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release with uncommitted changes")));
            assertThat(mee.output, oneOf(containsString(" * untracked.txt")));
            assertThat(mee.output, oneOf(containsString(" * someFolder/anotherUntracked.txt")));
        }
    }

    @Test
    public void failsIfThereAreUncommittedFiles() throws IOException, InterruptedException, GitAPIException {
        TestProject testProject = TestProject.singleModuleProject();
        new File(testProject.localDir, "uncommitted.txt").createNewFile();
//        new File(testProject.localDir, "uncommitted.txt").createNewFile();
        testProject.local.add().addFilepattern("uncommitted.txt").call();
        try {
            testProject.mvnRelease("1");
            Assert.fail("Should not have worked as there are uncommitted files");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release with uncommitted changes")));
            assertThat(mee.output, oneOf(containsString(" * uncommitted.txt")));
        }
    }

    @Test
    public void failsIfThereAreDependenciesOnSnapshotVersionsThatAreNotPartOfTheReactor() throws Exception {
        // Install the snapshot dependency so that it can be built
        TestProject dependency = TestProject.independentVersionsProject();
        dependency.mvn("install");

        TestProject badOne = TestProject.moduleWithSnapshotDependencies();

        badOne.mvn("install"); // this should work as the snapshot dependency is in the local repo

        try {
            badOne.mvnRelease("1");
            Assert.fail("Should not have worked as there are snapshot dependencies");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release with references to snapshot dependencies")));
            assertThat(mee.output, oneOf(containsString("The following dependency errors were found:")));
            assertThat(mee.output, oneOf(containsString(" * The parent of snapshot-dependencies is independent-versions 1.0-SNAPSHOT")));
            assertThat(mee.output, oneOf(containsString(" * snapshot-dependencies references dependency core-utils 2.0-SNAPSHOT")));
        }

        assertThat(badOne.local, GitMatchers.hasCleanWorkingDirectory());
    }

}
