package e2e;

import scaffolding.MavenExecutionException;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ValidationTest {

    @Rule
    public TestProject singleProject              = new TestProject(ProjectType.SINGLE);
    @Rule
    public TestProject independentVersionsProject = new TestProject(ProjectType.INDEPENDENT_VERSIONS);

    @Test
    public void failsIfThereAreUntrackedFiles() throws IOException, InterruptedException {
        new File(singleProject.localDir, "untracked.txt").createNewFile();
        new File(singleProject.localDir, "someFolder").mkdir();
        new File(singleProject.localDir, "someFolder/anotherUntracked.txt").createNewFile();
        try {
            singleProject.mvnRelease();
            Assert.fail("Should not have worked the second time");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release with uncommitted changes")));
            assertThat(mee.output, oneOf(containsString(" * untracked.txt")));
            assertThat(mee.output, oneOf(containsString(" * someFolder/anotherUntracked.txt")));
        }
    }

    @Test
    public void failsIfThereAreUncommittedFiles() throws IOException, InterruptedException, GitAPIException {
        new File(singleProject.localDir, "uncommitted.txt").createNewFile();
        singleProject.local.add().addFilepattern("uncommitted.txt").call();
        try {
            singleProject.mvnRelease();
            Assert.fail("Should not have worked as there are uncommitted files");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release with uncommitted changes")));
            assertThat(mee.output, oneOf(containsString(" * uncommitted.txt")));
        }
    }

    @Test
    public void ifIOErrorOccursWhileUpdatingPomsThenThisIsReported() throws IOException, InterruptedException {
        File pom = new File(independentVersionsProject.localDir, "console-app/pom.xml");
        pom.setWritable(false); // this should cause an IO exception when writing the pom
        try {
            independentVersionsProject.mvnRelease();
            Assert.fail("It was expected that this would fail due to a pom being readonly.");
        } catch (MavenExecutionException e) {
            assertThat(e.output,
                       twoOf(containsString("Unexpected exception while setting the release versions in the pom")));
            assertThat(e.output, oneOf(containsString("Going to revert changes because there was an error")));
        }
        assertThat(independentVersionsProject.local, hasCleanWorkingDirectory());
    }

    @Test
    public void failsIfThereAreDependenciesOnSnapshotVersionsThatAreNotPartOfTheReactor() throws Exception {
        // Install the snapshot dependency so that it can be built
        independentVersionsProject.mvn("install");

        TestProject badOne = TestProject.project(ProjectType.SNAPSHOT_DEPENDENCIES);

        badOne.mvn("install"); // this should work as the snapshot dependency is in the local repo

        try {
            badOne.mvnRelease();
            Assert.fail("Should not have worked as there are snapshot dependencies");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release with references to snapshot dependencies")));
            assertThat(mee.output, oneOf(containsString("The following dependency errors were found:")));
            assertThat(mee.output,
                       oneOf(containsString(" * The parent of snapshot-dependencies is independent-versions")));
            assertThat(mee.output, oneOf(containsString(" * snapshot-dependencies references dependency core-utils")));

            // commented out because this plugin is allowed to be a snapshot for testing purposes only
            //            assertThat(mee.output, oneOf(containsString(" * snapshot-dependencies references plugin multi-module-maven-release-plugin 0.2-SNAPSHOT")));
        }

        assertThat(badOne.local, hasCleanWorkingDirectory());
    }

    @Test
    public void failsIfThereAreDependenciesOnSnapshotVersionsWithVersionPropertiesThatAreNotPartOfTheReactor() throws
                                                                                                               Exception {
        // Install the snapshot dependency so that it can be built
        independentVersionsProject.mvn("install");

        TestProject badOne = TestProject.project(ProjectType.SNAPSHOT_DEPENDENCIES_VIA_PROPERTIES);

        badOne.mvn("install"); // this should work as the snapshot dependency is in the local repo

        try {
            badOne.mvnRelease();
            Assert.fail("Should not have worked as there are snapshot dependencies");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output, twoOf(containsString("Cannot release with references to snapshot dependencies")));
            assertThat(mee.output, oneOf(containsString("The following dependency errors were found:")));
            assertThat(mee.output, oneOf(
                containsString(" * snapshot-dependencies-with-version-properties references dependency core-utils")));
        }

        assertThat(badOne.local, hasCleanWorkingDirectory());
    }
}
