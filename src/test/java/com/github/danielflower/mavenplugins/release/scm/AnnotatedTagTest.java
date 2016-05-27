package com.github.danielflower.mavenplugins.release.scm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.version.Version;

import scaffolding.TestProject;

public class AnnotatedTagTest {
	private final Log log = mock(Log.class);
	private final GitFactory gitFactory = mock(GitFactory.class);
	private final GitRepository repo = new GitRepository();

	@Before
	public void setup() {
		repo.setGitFactory(gitFactory);
		repo.setLog(log);
	}

	@Test
	public void gettersReturnValuesPassedIn() throws Exception {
		// yep, testing getters... but only because it isn't a simple POJO
		final TestProject project = TestProject.singleModuleProject();
		when(gitFactory.newGit()).thenReturn(project.local);
		final ProposedTagsBuilder builder = repo.newProposedTagsBuilder(null);
		final Version ver = mock(Version.class);
		when(ver.getBusinessVersion()).thenReturn("the-version");
		when(ver.getBuildNumber()).thenReturn(2134l);
		builder.add("my-name", ver);
		final ProposedTag tag = builder.build().getTag("my-name", ver);
		assertEquals("my-name", tag.name());
		assertEquals("the-version", tag.getBusinessVersion());
		assertEquals(2134l, tag.getBuildNumber());
	}

	@Test
	public void aTagCanBeCreatedFromAGitTag() throws Exception {
		final TestProject project = TestProject.singleModuleProject();
		when(gitFactory.newGit()).thenReturn(project.local);
		final GitRepository repo = new GitRepository();
		repo.setGitFactory(gitFactory);
		repo.setLog(log);
		final ProposedTagsBuilder builder = repo.newProposedTagsBuilder(null);
		final Version ver = mock(Version.class);
		when(ver.getBusinessVersion()).thenReturn("the-version");
		when(ver.getBuildNumber()).thenReturn(2134l);
		builder.add("my-name", ver);
		final ProposedTag tag = builder.build().getTag("my-name", ver);
		tag.saveAtHEAD();

		final Ref ref = project.local.tagList().call().get(0);
		final ProposedTag inflatedTag = repo.fromRef(ref);
		assertThat(inflatedTag.name(), equalTo("my-name"));
		assertThat(inflatedTag.getBusinessVersion(), equalTo("the-version"));
		assertThat(inflatedTag.getBuildNumber(), equalTo(2134L));
	}

	@Test
	public void ifATagIsSavedWithoutJsonThenTheVersionIsSetTo0Dot0() throws Exception {
		final TestProject project = TestProject.singleModuleProject();
		project.local.tag().setName("my-name-1.0.2").setAnnotated(true).setMessage("This is not json").call();

		final Ref ref = project.local.tagList().call().get(0);
		when(gitFactory.newGit()).thenReturn(project.local);
		final GitRepository repo = new GitRepository();
		repo.setGitFactory(gitFactory);
		repo.setLog(log);
		final ProposedTag inflatedTag = repo.fromRef(ref);
		assertThat(inflatedTag.name(), equalTo("my-name-1.0.2"));
		assertThat(inflatedTag.getBusinessVersion(), equalTo("0"));
		assertThat(inflatedTag.getBuildNumber(), equalTo(0L));
	}

}
