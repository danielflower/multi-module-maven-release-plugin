package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
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
import static scaffolding.MvnRunner.runMaven;

public class NotRunInGitRepoTest {


    private static File projectRoot;

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException, IOException, GitAPIException {
        MvnRunner.installReleasePluginToLocalRepo();
        projectRoot = Photocopier.copyTestProjectToTemporaryLocation("single-module");
    }

    @Test
    public void ifTheReleaseIsRunFromANonGitRepoThenAnErrorIsClearlyDisplayed() throws IOException {
        try {
            runMaven(projectRoot, "releaser:release");
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {
            assertThat(e.output, twoOf(containsString("Releases can only be performed from Git repositories.")));
            assertThat(e.output, oneOf(containsString(projectRoot.getCanonicalPath() + " is not a Git repository.")));
        }

    }
}
