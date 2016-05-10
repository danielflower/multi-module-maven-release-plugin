package com.github.danielflower.mavenplugins.release.scm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.lib.Ref;
import org.junit.Test;

import scaffolding.TestProject;

public class AnnotatedTagTest {
	private final Log log = mock(Log.class);
	private final GitFactory gitFactory = mock(GitFactory.class);
	private final GitRepository repo = new GitRepository(log, gitFactory);

	@Test
	public void gettersReturnValuesPassedIn() throws Exception {
		// yep, testing getters... but only because it isn't a simple POJO
		final TestProject project = TestProject.singleModuleProject();
		when(gitFactory.newGit()).thenReturn(project.local);
		final ProposedTagsBuilder builder = repo.newProposedTagsBuilder(null);
		builder.add("my-name", "the-version", 2134);
		final ProposedTag tag = builder.build().getTag("my-name", "the-version", 2134);
		assertThat(tag.name(), equalTo("my-name"));
		assertThat(tag.version(), equalTo("the-version"));
		assertThat(tag.buildNumber(), equalTo(2134L));
	}

	@Test
	public void aTagCanBeCreatedFromAGitTag() throws Exception {
		final TestProject project = TestProject.singleModuleProject();
		when(gitFactory.newGit()).thenReturn(project.local);
		final GitRepository repo = new GitRepository(log, gitFactory);
		final ProposedTagsBuilder builder = repo.newProposedTagsBuilder(null);
		builder.add("my-name", "the-version", 2134);
		final ProposedTag tag = builder.build().getTag("my-name", "the-version", 2134);
		tag.saveAtHEAD();

		final Ref ref = project.local.tagList().call().get(0);
		final ProposedTag inflatedTag = repo.fromRef(ref);
		assertThat(inflatedTag.name(), equalTo("my-name"));
		assertThat(inflatedTag.version(), equalTo("the-version"));
		assertThat(inflatedTag.buildNumber(), equalTo(2134L));
	}

	@Test
	public void ifATagIsSavedWithoutJsonThenTheVersionIsSetTo0Dot0() throws Exception {
		final TestProject project = TestProject.singleModuleProject();
		project.local.tag().setName("my-name-1.0.2").setAnnotated(true).setMessage("This is not json").call();

		final Ref ref = project.local.tagList().call().get(0);
		when(gitFactory.newGit()).thenReturn(project.local);
		final GitRepository repo = new GitRepository(log, gitFactory);
		final ProposedTag inflatedTag = repo.fromRef(ref);
		assertThat(inflatedTag.name(), equalTo("my-name-1.0.2"));
		assertThat(inflatedTag.version(), equalTo("0"));
		assertThat(inflatedTag.buildNumber(), equalTo(0L));
	}

}
