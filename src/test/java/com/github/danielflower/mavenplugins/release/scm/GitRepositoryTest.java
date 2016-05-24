package com.github.danielflower.mavenplugins.release.scm;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Test;

public class GitRepositoryTest {
	private static final String ANY_REMOTE_URL = "anyRemoteUrl";
	private final Log log = mock(Log.class);
	private final GitFactory gitFactory = mock(GitFactory.class);
	private final GitRepository repository = new GitRepository();
	private final Git git = mock(Git.class);
	private final Ref ref = mock(Ref.class);

	@Before
	public void setup() throws SCMException {
		when(gitFactory.newGit()).thenReturn(git);
		repository.setGitFactory(gitFactory);
		repository.setLog(log);
	}

	@Test
	public void checkValidRefName() throws SCMException {
		// This should be ok
		repository.checkValidRefName("1.0.0");

		try {
			repository.checkValidRefName("\\");
			fail("Exception expected");
		} catch (final SCMException expected) {
			expected.getMessage().equals(format(GitRepository.INVALID_REF_NAME_MESSAGE, "\\"));
		}
	}

	@Test
	public void canDetectRemoteTags() throws Exception {
		final LsRemoteCommand cmd = mock(LsRemoteCommand.class);
		when(git.lsRemote()).thenReturn(cmd);
		when(cmd.setTags(true)).thenReturn(cmd);
		when(cmd.setHeads(false)).thenReturn(cmd);
		when(cmd.call()).thenReturn(Arrays.asList(ref));
		final Collection<Ref> refs = repository.allRemoteTags(ANY_REMOTE_URL);
		assertEquals(1, refs.size());
		assertEquals(ref, refs.iterator().next());
		verify(cmd).setRemote(ANY_REMOTE_URL);
	}
}
