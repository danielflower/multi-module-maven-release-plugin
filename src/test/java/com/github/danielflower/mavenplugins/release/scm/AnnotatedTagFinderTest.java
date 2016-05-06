package com.github.danielflower.mavenplugins.release.scm;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.ValidationException;

import scaffolding.TestProject;

public class AnnotatedTagFinderTest {
	private static final Log log = mock(Log.class);

	@Test
	public void findsTheLatestCommitWhereThereHaveBeenNoBranches() throws Exception {
		final TestProject project = TestProject.independentVersionsProject();
		final GitRepository repo = new GitRepository(log, project.local, null);

		final ProposedTag tag1 = saveFileInModule(project, "console-app", "1.2", 3);
		final ProposedTag tag2 = saveFileInModule(project, "core-utils", "2", 0);
		final ProposedTag tag3 = saveFileInModule(project, "console-app", "1.2", 4);

		assertThat(repo.tagsForVersion("console-app", "1.3"), hasSize(0));
		assertThat(repo.tagsForVersion("console-app", "1.2"), containsInAnyOrder(tag1, tag3));
		assertThat(repo.tagsForVersion("core-utils", "2"), contains(tag2));
	}

	static ProposedTag saveFileInModule(final TestProject project, final String moduleName, final String version,
			final long buildNumber) throws IOException, GitAPIException {
		project.commitRandomFile(moduleName);
		final String nameForTag = moduleName.equals(".") ? "root" : moduleName;
		return tagLocalRepo(project, nameForTag + "-" + version + "." + buildNumber, version, buildNumber);
	}

	private static ProposedTag tagLocalRepo(final TestProject project, final String tagName, final String version,
			final long buildNumber) throws GitAPIException {
		final GitRepository repo = new GitRepository(log, project.local, null);
		final ProposedTag tag = repo.create(tagName, version, buildNumber);
		tag.saveAtHEAD();
		return tag;
	}

	@Test
	public void canRecogniseTagsThatArePotentiallyOfTheSameVersion() {
		assertThat(GitRepository.isPotentiallySameVersionIgnoringBuildNumber("my-artifact-1.2",
				"refs/tags/my-artifact-1.2.2"), is(true));
		assertThat(GitRepository.isPotentiallySameVersionIgnoringBuildNumber("my-artifact-1.2",
				"refs/tags/my-artifact-1.20.2"), is(false));
		assertThat(GitRepository.isPotentiallySameVersionIgnoringBuildNumber("my-artifact-1.3",
				"refs/tags/my-artifact-1.2.2"), is(false));
		assertThat(GitRepository.isPotentiallySameVersionIgnoringBuildNumber("not-my-artifact-1.2",
				"refs/tags/my-artifact-1.2.2"), is(false));
	}

	@Test
	public void returnsMultipleTagsOnASingleCommit()
			throws IOException, GitAPIException, MojoExecutionException, ValidationException {
		final TestProject project = TestProject.independentVersionsProject();
		final GitRepository repo = new GitRepository(log, project.local, null);
		saveFileInModule(project, "console-app", "1.2", 1);
		final ProposedTag tag1 = tagLocalRepo(project, "console-app-1.1.1.1", "1.1.1", 1);
		final ProposedTag tag3 = tagLocalRepo(project, "console-app-1.1.1.3", "1.1.1", 3);
		final ProposedTag tag2 = tagLocalRepo(project, "console-app-1.1.1.2", "1.1.1", 2);
		final List<ProposedTag> annotatedTags = repo.tagsForVersion("console-app", "1.1.1");
		assertThat(annotatedTags, containsInAnyOrder(tag1, tag2, tag3));
	}
}
