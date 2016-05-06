package com.github.danielflower.mavenplugins.release.scm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.Test;

import scaffolding.TestProject;

public class AnnotatedTagTest {
	private final Log log = mock(Log.class);
	private final Git git = mock(Git.class);
	private final GitRepository repo = new GitRepository(log, git, null);

	@Test
	public void gettersReturnValuesPassedIn() throws Exception {
		// yep, testing getters... but only because it isn't a simple POJO
		final AnnotatedTag tag = repo.create("my-name", "the-version", 2134);
		assertThat(tag.name(), equalTo("my-name"));
		assertThat(tag.version(), equalTo("the-version"));
		assertThat(tag.buildNumber(), equalTo(2134L));
	}

	@Test
	public void aTagCanBeCreatedFromAGitTag() throws GitAPIException, IOException {
		final TestProject project = TestProject.singleModuleProject();
		final GitRepository repo = new GitRepository(log, project.local, null);
		final AnnotatedTag tag = repo.create("my-name", "the-version", 2134);
		tag.saveAtHEAD(project.local);

		final Ref ref = project.local.tagList().call().get(0);
		final AnnotatedTag inflatedTag = repo.fromRef(ref);
		assertThat(inflatedTag.name(), equalTo("my-name"));
		assertThat(inflatedTag.version(), equalTo("the-version"));
		assertThat(inflatedTag.buildNumber(), equalTo(2134L));
	}

	@Test
	public void ifATagIsSavedWithoutJsonThenTheVersionIsSetTo0Dot0() throws GitAPIException, IOException {
		final TestProject project = TestProject.singleModuleProject();
		project.local.tag().setName("my-name-1.0.2").setAnnotated(true).setMessage("This is not json").call();

		final Ref ref = project.local.tagList().call().get(0);
		final GitRepository repo = new GitRepository(log, project.local, null);
		final AnnotatedTag inflatedTag = repo.fromRef(ref);
		assertThat(inflatedTag.name(), equalTo("my-name-1.0.2"));
		assertThat(inflatedTag.version(), equalTo("0"));
		assertThat(inflatedTag.buildNumber(), equalTo(0L));
	}

}
