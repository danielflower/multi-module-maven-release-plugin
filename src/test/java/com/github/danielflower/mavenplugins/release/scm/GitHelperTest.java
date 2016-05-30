package com.github.danielflower.mavenplugins.release.scm;

import static com.github.danielflower.mavenplugins.release.scm.GitHelper.REFS_TAGS;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Test;

public class GitHelperTest {
	private static final String TAG_TO_CHECK = "tagToCheck";
	private final Git git = mock(Git.class);
	private final ListTagCommand cmd = mock(ListTagCommand.class);
	private final Ref ref = mock(Ref.class);

	@Before
	public void setup() throws Exception {
		when(git.tagList()).thenReturn(cmd);
		when(cmd.call()).thenReturn(asList(ref));
		when(ref.getName()).thenReturn(format("%s%s", REFS_TAGS, TAG_TO_CHECK));
	}

	@Test
	public void hasLocalTag() throws Exception {
		assertTrue(GitHelper.hasLocalTag(git, TAG_TO_CHECK));
		assertFalse(GitHelper.hasLocalTag(git, "someOtherTag"));
	}
}
