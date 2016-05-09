package com.github.danielflower.mavenplugins.release.scm;

import static com.github.danielflower.mavenplugins.release.scm.SCMRepositoryProvider.getRemoteUrlOrNullIfNoneSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.junit.Before;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.ValidationException;

public class SCMRepositoryProviderTest {
	private static final String DEVELOPER_CONNECTION = "scm:git:ssh://some/developerPath";
	private static final String CONNECTION = "scm:git:ssh://some/commonPath";
	private final Log log = mock(Log.class);
	private final GitFactory gitFactory = mock(GitFactory.class);
	private final Git git = mock(Git.class);
	private final MavenProject project = mock(MavenProject.class);
	private final Scm scm = mock(Scm.class);

	@Before
	public void setup() throws Exception {
		when(gitFactory.newGit()).thenReturn(git);
		when(project.getScm()).thenReturn(scm);

		final StatusCommand cmd = mock(StatusCommand.class);
		final Status status = mock(Status.class);
		when(status.isClean()).thenReturn(true);
		when(cmd.call()).thenReturn(status);
		when(git.status()).thenReturn(cmd);
	}

	@Test
	public void getRemoteUrlScmIsNull() throws ValidationException {
		assertNull(getRemoteUrlOrNullIfNoneSet(null));
	}

	@Test
	public void getRemoteUrlNoConnectionsOnScm() throws ValidationException {
		assertNull(getRemoteUrlOrNullIfNoneSet(scm));
	}

	@Test
	public void getRemoteUrlUseDeveloperConnection() throws ValidationException {
		when(scm.getDeveloperConnection()).thenReturn(DEVELOPER_CONNECTION);
		when(scm.getConnection()).thenReturn(CONNECTION);
		assertEquals("ssh://some/developerPath", getRemoteUrlOrNullIfNoneSet(scm));
	}

	@Test
	public void getRemoteUrlUseConnection() throws ValidationException {
		when(scm.getConnection()).thenReturn(CONNECTION);
		assertEquals("ssh://some/commonPath", getRemoteUrlOrNullIfNoneSet(scm));
	}

	@Test
	public void getRemoteUrlIllegalProtocol() {
		when(scm.getDeveloperConnection()).thenReturn("scm:svn:ssh//some/illegal/protocol");
		try {
			getRemoteUrlOrNullIfNoneSet(scm);
			fail("Exception expected");
		} catch (final ValidationException expected) {
			assertEquals(
					"Cannot run the release plugin with a non-Git version control system scm:svn:ssh//some/illegal/protocol",
					expected.getMessage());
			final List<String> messages = expected.getMessages();
			assertEquals(2, messages.size());
			assertEquals("Cannot run the release plugin with a non-Git version control system", messages.get(0));
			assertEquals("The value in your scm tag is scm:svn:ssh//some/illegal/protocol", messages.get(1));
		}
	}

	@Test
	public void verifyRealRepository() throws ValidationException {
		final SCMRepositoryProvider provider = new SCMRepositoryProvider(log, gitFactory, project);
		assertFalse(Proxy.isProxyClass(provider.get().getClass()));

		// Should not throw an exception
		provider.get().errorIfNotClean();
	}

	@Test
	public void verifyErrorProxyRepository() throws ValidationException {
		final ValidationException expected = new ValidationException("", Collections.<String> emptyList());
		doThrow(expected).when(gitFactory).newGit();

		final SCMRepositoryProvider provider = new SCMRepositoryProvider(log, gitFactory, project);
		assertTrue(Proxy.isProxyClass(provider.get().getClass()));

		try {
			provider.get().errorIfNotClean();
			fail("Exception expected");
		} catch (final ValidationException e) {
			assertSame(expected, e);
		}
	}
}
