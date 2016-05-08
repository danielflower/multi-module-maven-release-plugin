package e2e;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.Photocopier;
import scaffolding.TestProject;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;

public class GitRelatedTest {

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException, IOException, GitAPIException {
        MvnRunner.installReleasePluginToLocalRepo();

    }

    @Test
    public void ifTheReleaseIsRunFromANonGitRepoThenAnErrorIsClearlyDisplayed() throws IOException {
        File projectRoot = Photocopier.copyTestProjectToTemporaryLocation("single-module");
        TestProject.performPomSubstitution(projectRoot);
        try {
            new MvnRunner().runMaven(projectRoot, "releaser:release");
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {
            assertThat(e.output, twoOf(containsString("Releases can only be performed from Git repositories.")));
            assertThat(e.output, oneOf(containsString(projectRoot.getCanonicalPath() + " is not a Git repository.")));
        }
    }

    @Test
    public void ifThereIsNoScmInfoAndNoRemoteBranchThenAnErrorIsThrown() throws GitAPIException, IOException, InterruptedException {
        TestProject testProject = TestProject.singleModuleProject();

        StoredConfig config = testProject.local.getRepository().getConfig();
        config.unsetSection("remote", "origin");
        config.save();

        try {
            testProject.mvnRelease("1");
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {
            assertThat(e.output, oneOf(containsString("[ERROR] origin: not found.")));
        }
    }

    @Test
    public void ifTheScmIsSpecifiedButIsNotGitThenThisIsThrown() throws GitAPIException, IOException, InterruptedException {
        TestProject testProject = TestProject.moduleWithScmTag();
        File pom = new File(testProject.localDir, "pom.xml");
        String xml = FileUtils.readFileToString(pom, "UTF-8");
        xml = xml.replace("scm:git:", "scm:svn:");
        FileUtils.writeStringToFile(pom, xml, "UTF-8");
        testProject.local.add().addFilepattern("pom.xml").call();
        testProject.local.commit().setMessage("Changing pom for test").call();

        try {
            testProject.mvnRelease("1");
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {
            assertThat(e.output, twoOf(containsString("Cannot run the release plugin with a non-Git version control system")));
            assertThat(e.output, oneOf(containsString("The value in your scm tag is scm:svn:")));
        }
    }

    @Test
    public void ifThereIsNoRemoteButTheScmDetailsArePresentThenThisIsUsed() throws GitAPIException, IOException, InterruptedException {
        TestProject testProject = TestProject.moduleWithScmTag();

        StoredConfig config = testProject.local.getRepository().getConfig();
        config.unsetSection("remote", "origin");
        config.save();

        testProject.mvnRelease("1");
    }

}
