package e2e;

import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTag;
import static scaffolding.GitMatchers.hasTagWithModuleVersion;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.TestUtils;
import de.hilling.maven.release.releaseinfo.ReleaseInfoStorage;
import de.hilling.maven.release.versioning.ImmutableFixVersion;
import de.hilling.maven.release.versioning.ImmutableModuleVersion;
import de.hilling.maven.release.versioning.ImmutableReleaseInfo;
import de.hilling.maven.release.versioning.ReleaseInfo;

public class SingleModuleTest {

    private static final String expected = "1.0";

    @Rule
    public TestProject testProject = new TestProject(ProjectType.SINGLE);

    @Test
    public void canUpdateSnapshotVersionToReleaseVersionAndInstallToLocalRepo() throws Exception {
        List<String> outputLines = testProject.mvnRelease();
        assertThat(outputLines, oneOf(containsString("Going to release single-module " + expected)));
        assertThat(outputLines, oneOf(containsString("Hello from version " + expected + "!")));

        MvnRunner
            .assertArtifactInLocalRepo(TestUtils.TEST_GROUP_ID, "single-module", expected);

        assertThat(new File(testProject.localDir, "target/single-module-" + expected + "-package.jar").exists(),
                   is(true));
    }

    @Test
    public void theReleaseNumbersWillStartAt0AndThenIncrement() throws IOException, GitAPIException {
        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "single-module", "1.0"));
        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "single-module", "1.1"));
        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "single-module", "1.2"));
    }

    @Test
    public void theReleaseNumbersWillStartAt0AndThenIncrementTakingIntoAccountManuallyUpdatedReleaseInfoFiles() throws
                                                                                                                Exception {
        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "single-module", "1.0"));

        final ReleaseInfoStorage infoStorage = new ReleaseInfoStorage(testProject.localDir, testProject.local);
        final ReleaseInfo currentInfo = infoStorage.load();
        final ImmutableReleaseInfo.Builder releaseBuilder = ImmutableReleaseInfo.builder().from(currentInfo);
        final ImmutableModuleVersion currentModuleVersion = currentInfo.getModules().get(0);
        final ImmutableModuleVersion.Builder moduleInfo = ImmutableModuleVersion.builder().from(currentModuleVersion);
        final ImmutableFixVersion.Builder versionBuilder = ImmutableFixVersion.builder()
                                                                              .from(currentModuleVersion.getVersion());
        releaseBuilder.modules(singletonList(moduleInfo.version(versionBuilder.minorVersion(5L).build()).build()));
        infoStorage.store(releaseBuilder.build());

        testProject.mvn("releaser:release");
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "single-module", "1.6"));
    }

    @Test
    public void theTagNameIsActuallyStoredInReleaseInfo() throws Exception {
        testProject.mvnRelease();
        final ReleaseInfo currentInfo = currentReleaseInfo();
        String expectedTag = expectedTag();
        assertThat(testProject.local, hasTag(expectedTag));
        assertThat(testProject.origin, hasTag(expectedTag));
    }

    public String expectedTag() {
        return currentReleaseInfo().getTagName().get();
    }

    public ReleaseInfo currentReleaseInfo() {
        final ReleaseInfoStorage infoStorage = new ReleaseInfoStorage(testProject.localDir, testProject.local);
        try {
            return infoStorage.load();
        } catch (MojoExecutionException e) {
            throw new RuntimeException("info access failed");
        }
    }

    @Test
    public void onlyLocalGitRepoIsTaggedWithoutPush() throws IOException, InterruptedException {
        testProject.mvn("-Dpush=false", "releaser:release");
        assertThat(testProject.local, hasTag(expectedTag()));
        assertThat(testProject.origin, not(hasTag(expectedTag())));
    }

    // TODO
    @Ignore("update test")
    @Test
    public void thePomChangesAreRevertedAfterTheRelease() throws IOException, InterruptedException {
        ObjectId originHeadAtStart = head(testProject.origin);
        ObjectId localHeadAtStart = head(testProject.local);
        assertThat(originHeadAtStart, equalTo(localHeadAtStart));
        testProject.mvnRelease();
        assertThat(testProject.local, hasCleanWorkingDirectory());
        assertThat(head(testProject.origin), equalTo(originHeadAtStart));
        assertThat(head(testProject.local), equalTo(localHeadAtStart));
    }

    private ObjectId head(Git git) throws IOException {
        return git.getRepository().getRef("HEAD").getObjectId();
    }
}
