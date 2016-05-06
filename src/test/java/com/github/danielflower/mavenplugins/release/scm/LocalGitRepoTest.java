package com.github.danielflower.mavenplugins.release.scm;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static scaffolding.TestProject.dirToGitScmReference;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.Test;

import scaffolding.TestProject;

public class LocalGitRepoTest {
	private final Log log = mock(Log.class);
	TestProject project = TestProject.singleModuleProject();
	private final GitRepository repo = new GitRepository(log, project.local, null);

	@Test
	public void canDetectLocalTags() throws GitAPIException {
		final GitRepository repo = new GitRepository(log, project.local, null);
		tag(project.local, "some-tag");
		assertThat(repo.hasLocalTag("some-tag"), is(true));
		assertThat(repo.hasLocalTag("some-ta"), is(false));
		assertThat(repo.hasLocalTag("some-tagyo"), is(false));
	}

	@Test
	public void canDetectRemoteTags() throws Exception {
		final GitRepository repo = new GitRepository(log, project.local, null);
		tag(project.origin, "some-tag");
		assertThat(repo.remoteTagsFrom(tags("blah", "some-tag")), equalTo(asList("some-tag")));
		assertThat(repo.remoteTagsFrom(tags("blah", "some-taggart")), equalTo(emptyList()));
	}

	@Test
	public void usesThePassedInScmUrlToFindRemote() throws Exception {
		final Scm scm = mock(Scm.class);
		final String remote = dirToGitScmReference(project.originDir);
		when(scm.getDeveloperConnection()).thenReturn(remote);
		final GitRepository repo = new GitRepository(log, project.local,
				SCMRepositoryProvider.getRemoteUrlOrNullIfNoneSet(scm));
		tag(project.origin, "some-tag");

		final StoredConfig config = project.local.getRepository().getConfig();
		config.unsetSection("remote", "origin");
		config.save();

		assertThat(repo.remoteTagsFrom(tags("blah", "some-tag")), equalTo(asList("some-tag")));
	}

	@Test
	public void canHaveManyTags() throws GitAPIException {
		final int numberOfTags = 50; // setting this to 1000 works but takes too
										// long
		for (int i = 0; i < numberOfTags; i++) {
			tag(project.local, "this-is-a-tag-" + i);
		}
		project.local.push().setPushTags().call();
		final GitRepository repo = new GitRepository(log, project.local, null);
		for (int i = 0; i < numberOfTags; i++) {
			final String tagName = "this-is-a-tag-" + i;
			assertThat(repo.hasLocalTag(tagName), is(true));
			assertThat(repo.remoteTagsFrom(tags(tagName)).size(), is(1));
		}
	}

	private List<AnnotatedTag> tags(final String... tagNames) {
		final List<AnnotatedTag> tags = new ArrayList<AnnotatedTag>();
		for (final String tagName : tagNames) {
			tags.add(repo.create(tagName, "1", 0));
		}
		return tags;
	}

	private static List<String> emptyList() {
		return new ArrayList<String>();
	}

	private static void tag(final Git repo, final String name) throws GitAPIException {
		repo.tag().setAnnotated(true).setName(name).setMessage("Some message").call();
	}
}
