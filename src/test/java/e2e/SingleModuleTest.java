package e2e;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTag;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.scm.GitRepository;
import com.github.danielflower.mavenplugins.release.scm.ProposedTags;
import com.github.danielflower.mavenplugins.release.scm.ProposedTagsBuilder;

import scaffolding.TestProject;

public class SingleModuleTest extends E2ETest {

	final String buildNumber = String.valueOf(System.currentTimeMillis());
	final String expected = "1.0." + buildNumber;
	final TestProject testProject = TestProject.singleModuleProject();

	@Test
	public void canUpdateSnapshotVersionToReleaseVersionAndInstallToLocalRepo() throws Exception {
		final List<String> outputLines = testProject.mvnRelease(buildNumber);
		assertThat(outputLines, oneOf(containsString("Going to release single-module " + expected)));
		assertThat(outputLines, oneOf(containsString("Hello from version " + expected + "!")));

		assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects", "single-module", expected);

		assertThat(new File(testProject.localDir, "target/single-module-" + expected + "-package.jar").exists(),
				is(true));
	}

	// TODO: Work with a Guice injector and make class GitRepository package
	// private
	@Test
	public void theBuildNumberIsOptionalAndWillStartAt0AndThenIncrementTakingIntoAccountLocalAndRemoteTags()
			throws Exception {
		testProject.mvn("releaser:release");
		assertThat(testProject.local, hasTag("single-module-1.0.0"));
		testProject.mvn("releaser:release");
		assertThat(testProject.local, hasTag("single-module-1.0.1"));

		final GitRepository repo = new GitRepository(mock(Log.class), testProject.local, null);
		final ProposedTagsBuilder builder = repo.newProposedTagsBuilder();
		builder.add("single-module-1.0.2", "1.0", 2).build().getTag("single-module-1.0.2", "1.0", 2).saveAtHEAD();
		testProject.mvn("releaser:release");
		assertThat(testProject.local, hasTag("single-module-1.0.3"));

		builder.add("single-module-1.0.4", "1.0", 4);
		builder.add("unrelated-module-1.0.5", "1.0", 5);
		final ProposedTags tags = builder.build();
		tags.getTag("single-module-1.0.4", "1.0", 4).saveAtHEAD();
		tags.getTag("unrelated-module-1.0.5", "1.0", 5).saveAtHEAD();

		testProject.mvn("releaser:release");
		assertThat(testProject.local, hasTag("single-module-1.0.5"));

	}

	@Test
	public void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion()
			throws IOException, InterruptedException {
		testProject.mvnRelease(buildNumber);
		final String expectedTag = "single-module-" + expected;
		assertThat(testProject.local, hasTag(expectedTag));
		assertThat(testProject.origin, hasTag(expectedTag));
	}

	@Test
	public void thePomChangesAreRevertedAfterTheRelease() throws IOException, InterruptedException {
		final ObjectId originHeadAtStart = head(testProject.origin);
		final ObjectId localHeadAtStart = head(testProject.local);
		assertThat(originHeadAtStart, equalTo(localHeadAtStart));
		testProject.mvnRelease(buildNumber);
		assertThat(head(testProject.origin), equalTo(originHeadAtStart));
		assertThat(head(testProject.local), equalTo(localHeadAtStart));
		assertThat(testProject.local, hasCleanWorkingDirectory());
	}

	private ObjectId head(final Git git) throws IOException {
		return git.getRepository().getRef("HEAD").getObjectId();
	}

}
