package e2e;

import com.github.danielflower.mavenplugins.release.AnnotatedTag;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTag;

public class SingleModuleTest {

    final String buildNumber = String.valueOf(System.currentTimeMillis());
    final String expected = "1.0." + buildNumber;
    final TestProject testProject = TestProject.singleModuleProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void canUpdateSnapshotVersionToReleaseVersionAndInstallToLocalRepo() throws Exception {
        List<String> outputLines = testProject.mvnRelease(buildNumber);
        assertThat(outputLines, oneOf(containsString("Going to release single-module " + expected)));
        assertThat(outputLines, oneOf(containsString("Hello from version " + expected + "!")));

        MvnRunner.assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects", "single-module", expected);

        assertThat(new File(testProject.localDir, "target/single-module-" + expected + "-package.jar").exists(), is(true));
    }

    @Test
    public void theBuildNumberIsOptionalAndWillStartAt0AndThenIncrementTakingIntoAccountLocalAndRemoteTags() throws IOException, GitAPIException {
        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTag("single-module-1.0.0"));
        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTag("single-module-1.0.1"));

        AnnotatedTag.create("single-module-1.0.2", "1.0", 2).saveAtHEAD(testProject.local);
        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTag("single-module-1.0.3"));

        AnnotatedTag.create("single-module-1.0.4", "1.0", 4).saveAtHEAD(testProject.origin);
        AnnotatedTag.create("unrelated-module-1.0.5", "1.0", 5).saveAtHEAD(testProject.origin);
        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTag("single-module-1.0.5"));

    }

    @Test
    public void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion() throws IOException, InterruptedException {
        testProject.mvnRelease(buildNumber);
        String expectedTag = "single-module-" + expected;
        assertThat(testProject.local, hasTag(expectedTag));
        assertThat(testProject.origin, hasTag(expectedTag));
    }

    @Test
    public void onlyLocalGitRepoIsTaggedWithTheModuleNameAndVersionWithoutPush() throws IOException, InterruptedException {
        testProject.mvn("-DbuildNumber=" + buildNumber,
                "-Dpush=false",
                "releaser:release");
        String expectedTag = "single-module-" + expected;
        assertThat(testProject.local, hasTag(expectedTag));
        assertThat(testProject.origin, not(hasTag(expectedTag)));
    }

    @Test
    public void thePomChangesAreRevertedAfterTheRelease() throws IOException, InterruptedException {
        ObjectId originHeadAtStart = head(testProject.origin);
        ObjectId localHeadAtStart = head(testProject.local);
        assertThat(originHeadAtStart, equalTo(localHeadAtStart));
        testProject.mvnRelease(buildNumber);
        assertThat(head(testProject.origin), equalTo(originHeadAtStart));
        assertThat(head(testProject.local), equalTo(localHeadAtStart));
        assertThat(testProject.local, hasCleanWorkingDirectory());
    }

    private ObjectId head(Git git) throws IOException {
        return git.getRepository().getRef("HEAD").getObjectId();
    }

}
