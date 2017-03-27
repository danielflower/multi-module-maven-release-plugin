package e2e;

import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;

import java.io.File;
import java.io.IOException;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ValidationTest {

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void ifTheSameVersionIsReleasedTwiceItErrorsLoudly() throws Exception {
        TestProject testProject = TestProject.singleModuleProject();
        testProject.mvnRelease("1");
        testProject.commitRandomFile(".").pushIt();
        try {
            testProject.mvnRelease("1");
            Assert.fail("Should not have completed running");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output,
                twoOf(containsString("There is already a tag named single-module-1.1 in this repository.")));
            assertThat(mee.output,
                oneOf(containsString("It is likely that this version has been released before.")));
            assertThat(mee.output,
                oneOf(containsString("Please try incrementing the build number and trying again.")));
        }
    }

    @Test
    public void ifAReleaseTagAlreadyExistsInTheRemoteRepoThenItErrorsEarly() throws Exception {
        TestProject testProject = TestProject.singleModuleProject();
        testProject.origin.tag()
            .setAnnotated(true).setName("single-module-1.1").setMessage("Simulating remote tag").call();
        try {
            testProject.mvnRelease("1");
            Assert.fail("Should not have completed running");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release because there is already a tag with the same build number on the remote Git repo.")));
            assertThat(mee.output, oneOf(containsString("* There is already a tag named single-module-1.1 in the remote repo.")));
            assertThat(mee.output, oneOf(containsString("Please try releasing again with a new build number.")));
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
    public void ifIOErrorOccursWhileUpdatingPomsThenThisIsReported() throws IOException, InterruptedException {
        TestProject testProject = TestProject.independentVersionsProject();
        File pom = new File(testProject.localDir, "console-app/pom.xml");
        pom.setWritable(false); // this should cause an IO exception when writing the pom
        try {
            testProject.mvnRelease("1");
            Assert.fail("It was expected that this would fail due to a pom being readonly.");
        } catch (MavenExecutionException e) {
            assertThat(e.output, twoOf(containsString("Unexpected exception while setting the release versions in the pom")));
            assertThat(e.output, oneOf(containsString("Going to revert changes because there was an error")));
        }
        assertThat(testProject.local, hasCleanWorkingDirectory());
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
            assertThat(mee.output, oneOf(containsString(" * The parent of snapshot-dependencies is independent-versions 1-SNAPSHOT")));
            assertThat(mee.output, oneOf(containsString(" * snapshot-dependencies references dependency core-utils 2-SNAPSHOT")));

            // commented out because this plugin is allowed to be a snapshot for testing purposes only
//            assertThat(mee.output, oneOf(containsString(" * snapshot-dependencies references plugin multi-module-maven-release-plugin 0.2-SNAPSHOT")));
        }

        assertThat(badOne.local, hasCleanWorkingDirectory());
    }

    @Test
    public void failsIfThereAreDependenciesOnSnapshotVersionsWithVersionPropertiesThatAreNotPartOfTheReactor() throws Exception {
        // Install the snapshot dependency so that it can be built
        TestProject dependency = TestProject.independentVersionsProject();
        dependency.mvn("install");
    	
        TestProject badOne = TestProject.moduleWithSnapshotDependenciesWithVersionProperties();

        badOne.mvn("install"); // this should work as the snapshot dependency is in the local repo

        try {
            badOne.mvnRelease("1");
            Assert.fail("Should not have worked as there are snapshot dependencies");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release with references to snapshot dependencies")));
            assertThat(mee.output, oneOf(containsString("The following dependency errors were found:")));
            assertThat(mee.output, oneOf(containsString(" * snapshot-dependencies-with-version-properties references dependency core-utils ${core-utils.version}")));
        }

        assertThat(badOne.local, hasCleanWorkingDirectory());
    }
}
