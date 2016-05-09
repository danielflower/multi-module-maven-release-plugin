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
import org.junit.Ignore;
import org.junit.Test;

import scaffolding.TestProject;

public class LocalGitRepoTest {
	private final Log log = mock(Log.class);
	TestProject project = TestProject.singleModuleProject();
	private GitRepository repo = new GitRepository(log, project.local, null);

	@Test
	public void canDetectLocalTags() throws GitAPIException {
		final GitRepository repo = new GitRepository(log, project.local, null);
		tag(project.local, "some-tag");
		assertThat(repo.hasLocalTag("some-tag"), is(true));
		assertThat(repo.hasLocalTag("some-ta"), is(false));
		assertThat(repo.hasLocalTag("some-tagyo"), is(false));
	}

	// Needs to be refactored because builder throws a ValidationException when
	// tag is already present.
	@Ignore
	@Test
	public void canDetectRemoteTags() throws Exception {
		tag(project.origin, "some-tag");
		assertThat(tags("blah", "some-tag").getMatchingRemoteTags(), equalTo(asList("some-tag")));
		assertThat(tags("blah", "some-taggart").getMatchingRemoteTags(), equalTo(emptyList()));
	}

	// Needs to be refactored because builder throws a ValidationException when
	// tag is already present.
	@Ignore
	@Test
	public void usesThePassedInScmUrlToFindRemote() throws Exception {
		final Scm scm = mock(Scm.class);
		final String remote = dirToGitScmReference(project.originDir);
		when(scm.getDeveloperConnection()).thenReturn(remote);
		repo = new GitRepository(log, project.local, SCMRepositoryProvider.getRemoteUrlOrNullIfNoneSet(scm));
		tag(project.origin, "some-tag");

		final StoredConfig config = project.local.getRepository().getConfig();
		config.unsetSection("remote", "origin");
		config.save();

		assertThat(tags("blah", "some-tag").getMatchingRemoteTags(), equalTo(asList("some-tag")));
	}

	// Needs to be refactored because builder throws a ValidationException when
	// tag is already present.
	@Ignore
	@Test
	public void canHaveManyTags() throws Exception {
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
			assertThat(tags(tagName).getMatchingRemoteTags().size(), is(1));
		}
	}

	private ProposedTags tags(final String... tagNames) throws Exception {
		final ProposedTagsBuilder builder = repo.newProposedTagsBuilder();
		for (final String tagName : tagNames) {
			builder.add(tagName, "1", 0);
		}
		return builder.build();
	}

	private static List<String> emptyList() {
		return new ArrayList<String>();
	}

	private static void tag(final Git repo, final String name) throws GitAPIException {
		repo.tag().setAnnotated(true).setName(name).setMessage("Some message").call();
	}
}
