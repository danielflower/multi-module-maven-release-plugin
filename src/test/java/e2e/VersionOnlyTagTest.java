package e2e;

import com.github.danielflower.mavenplugins.release.AnnotatedTag;
import org.apache.commons.io.FileUtils;
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
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTag;

public class VersionOnlyTagTest {

    final String buildNumber = String.valueOf(System.currentTimeMillis());
    final String expected = "2.0." + buildNumber;
    final TestProject testProject = TestProject.versionOnlyTagProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void canUpdateSnapshotVersionToReleaseVersionAndInstallToLocalRepo() throws Exception {
        List<String> outputLines = testProject.mvnRelease(buildNumber);
        assertThat(outputLines, oneOf(containsString("Going to release version-only-tag " + expected)));
        assertThat(outputLines, oneOf(containsString("Hello from version " + expected + "!")));

        MvnRunner.assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects", "version-only-tag", expected);

        assertThat(new File(testProject.localDir, "target/version-only-tag-" + expected + "-package.jar").exists(), is(true));
    }

    @Test
    public void theBuildNumberIsOptionalAndWillStartAt0AndThenIncrementTakingIntoAccountLocalAndRemoteTags() throws IOException, GitAPIException {
        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTag("2.0.0"));
        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTag("2.0.1"));

        AnnotatedTag.create("2.0.2", "2.0", 2).saveAtHEAD(testProject.local);
        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTag("2.0.3"));

        AnnotatedTag.create("2.0.4", "2.0", 4).saveAtHEAD(testProject.origin);
        AnnotatedTag.create("unrelated-module-2.0.5", "2.0", 5).saveAtHEAD(testProject.origin);
        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTag("2.0.5"));

    }

    @Test
    public void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion() throws IOException, InterruptedException {
        testProject.mvnRelease(buildNumber);
        String expectedTag = expected;
        assertThat(testProject.local, hasTag(expectedTag));
        assertThat(testProject.origin, hasTag(expectedTag));
    }

    @Test
    public void onlyLocalGitRepoIsTaggedWithTheModuleNameAndVersionWithoutPush() throws IOException, InterruptedException {
        testProject.mvn("-DbuildNumber=" + buildNumber,
                "-Dpush=false",
                "releaser:release");
        String expectedTag = expected;
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
        assertThat(FileUtils.readFileToString(new File(testProject.localDir, "pom.xml"), StandardCharsets.UTF_8),
            containsString("<!-- This comment is here for a test that ensures comments are not deleted -->"));
    }

    private ObjectId head(Git git) throws IOException {
        return git.getRepository().getRefDatabase().findRef("HEAD").getObjectId();
    }

    @Test
    public void originTagsNotConsultedWithoutPull() throws Exception {
        testProject.mvn("releaser:release");

        AnnotatedTag.create("2.0.2", "2.0", 2).saveAtHEAD(testProject.local);
        AnnotatedTag.create("2.0.5", "2.0", 5).saveAtHEAD(testProject.origin);

        testProject.mvn("-Dpush=false",
                        "-Dpull=false",
                        "releaser:release");
        assertThat(testProject.local, hasTag("2.0.3"));
        assertThat(testProject.origin, not(hasTag("2.0.3")));
    }

}
