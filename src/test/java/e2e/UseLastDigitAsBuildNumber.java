package e2e;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTag;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Test;

import scaffolding.TestProject;

public class UseLastDigitAsBuildNumber extends E2ETest {
	final String expected = "1.0.1";
	final TestProject testProject = TestProject.useLastDigitAsBuildNumber();

	@Test
	public void canUpdateSnapshotVersionToReleaseVersionAndInstallToLocalRepo() throws Exception {
		final List<String> outputLines = testProject.mvn("releaser:release");
		assertThat(outputLines, oneOf(containsString("Going to release use-last-digit-as-build-number " + expected)));
		assertThat(outputLines, oneOf(containsString("Hello from version " + expected + "!")));

		assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects", "use-last-digit-as-build-number",
				expected);

		assertThat(new File(testProject.localDir, "target/use-last-digit-as-build-number-" + expected + "-package.jar")
				.exists(), is(true));
	}

	@Test
	public void theBuildNumberIsBasedOnTheLastDigitOfTheVersion() throws Exception {
		testProject.mvn("releaser:release");
		assertThat(testProject.local, hasTag("use-last-digit-as-build-number-1.0.1"));
	}

	@Test
	public void theLocalAndRemoteGitReposAreTaggedWithTheModuleNameAndVersion()
			throws IOException, InterruptedException {
		testProject.mvn("releaser:release");
		final String expectedTag = "use-last-digit-as-build-number-" + expected;
		assertThat(testProject.local, hasTag(expectedTag));
		assertThat(testProject.origin, hasTag(expectedTag));
	}

	@Test
	public void thePomChangesAreRevertedAfterTheRelease() throws IOException, InterruptedException {
		final ObjectId originHeadAtStart = head(testProject.origin);
		final ObjectId localHeadAtStart = head(testProject.local);
		assertThat(originHeadAtStart, equalTo(localHeadAtStart));
		testProject.mvn("releaser:release");
		assertThat(head(testProject.origin), equalTo(originHeadAtStart));
		assertThat(head(testProject.local), equalTo(localHeadAtStart));
		assertThat(testProject.local, hasCleanWorkingDirectory());
	}

	private ObjectId head(final Git git) throws IOException {
		return git.getRepository().getRef("HEAD").getObjectId();
	}

}
