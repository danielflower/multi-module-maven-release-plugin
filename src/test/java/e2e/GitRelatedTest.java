package e2e;

import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.Photocopier;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.TestUtils;

public class GitRelatedTest {

    @Rule
    public TestProject testProject = new TestProject(ProjectType.SINGLE);
    @Rule
    public TestProject scmTagProject = new TestProject(ProjectType.TAGGED_MODULE);

    @Test
    public void ifTheReleaseIsRunFromANonGitRepoThenAnErrorIsClearlyDisplayed() throws IOException {
        File projectRoot = Photocopier.copyTestProjectToTemporaryLocation("single-module", UUID.randomUUID().toString());
        TestProject.performPomSubstitution(projectRoot);
        try {
            new MvnRunner().runMaven(projectRoot, TestUtils.RELEASE_GOAL);
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {
            assertThat(e.output, twoOf(containsString("Releases can only be performed from Git repositories.")));
            assertThat(e.output, oneOf(containsString(projectRoot.getCanonicalPath() + " is not a Git repository.")));
        }
    }

    @Test
    public void ifThereIsNoScmInfoAndNoRemoteBranchThenAnErrorIsThrown() throws GitAPIException, IOException, InterruptedException {

        StoredConfig config = testProject.local.getRepository().getConfig();
        config.unsetSection("remote", "origin");
        config.save();

        try {
            testProject.mvnRelease();
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {
            assertThat(e.output, oneOf(containsString("[ERROR] unable to list tags: origin: not found.")));
        }
    }

    @Test
    public void ifTheScmIsSpecifiedButIsNotGitThenThisIsThrown() throws GitAPIException, IOException, InterruptedException {
        File pom = new File(scmTagProject.localDir, "pom.xml");
        String xml = FileUtils.readFileToString(pom, "UTF-8");
        xml = xml.replace("scm:git:", "scm:svn:");
        FileUtils.writeStringToFile(pom, xml, "UTF-8");
        scmTagProject.local.add().addFilepattern("pom.xml").call();
        scmTagProject.local.commit().setMessage("Changing pom for test").call();

        try {
            scmTagProject.mvnRelease();
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {
            assertThat(e.output, twoOf(containsString("Cannot run the release plugin with a non-Git version control system")));
            assertThat(e.output, oneOf(containsString("The value in your scm tag is scm:svn:")));
        }
    }

    @Test
    public void ifThereIsNoRemoteButTheScmDetailsArePresentThenThisIsUsed() throws GitAPIException, IOException, InterruptedException {

        StoredConfig config = scmTagProject.local.getRepository().getConfig();
        config.unsetSection("remote", "origin");
        config.save();

        scmTagProject.mvnRelease();
    }

}
