package com.github.danielflower.mavenplugins.release.pom;

import static com.github.danielflower.mavenplugins.release.pom.DefaultChangeSet.REVERT_ERROR_MESSAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.scm.SCMException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

public class DefaultChangeSetTest {
	private final Log log = mock(Log.class);
	private final SCMRepository repository = mock(SCMRepository.class);
	private final DefaultChangeSet set = new DefaultChangeSet(log, repository);

	/**
	 * @throws Exception
	 */
	@Test
	public void closeNoFailureSetRevertSuccess() throws Exception {
		when(repository.revertChanges(set)).thenReturn(true);
		set.close();
		verify(repository).revertChanges(set);
		verify(log, never()).warn(REVERT_ERROR_MESSAGE);
	}

	@Test
	public void closeFailureSetRevertSuccess() throws Exception {
		when(repository.revertChanges(set)).thenReturn(true);
		final Exception expected = new Exception();
		set.setFailure(expected);
		try {
			set.close();
			fail("Exception expected!");
		} catch (final ChangeSetCloseException e) {
			assertSame(expected, e.getCause());
		}
		verify(log, never()).warn(REVERT_ERROR_MESSAGE);
	}

	@Test
	public void closeNoFailureSetRevertFailed() throws Exception {
		try {
			set.close();
			fail("Exception expected!");
		} catch (final ChangeSetCloseException e) {
			assertEquals(REVERT_ERROR_MESSAGE, e.getMessage());
		}
		verify(log, never()).warn(REVERT_ERROR_MESSAGE);
	}

	@Test
	public void closeFailureSetRevertFailed() throws Exception {
		final Exception expected = new Exception();
		set.setFailure(expected);
		try {
			set.close();
			fail("Exception expected!");
		} catch (final Exception e) {
			assertSame(expected, e.getCause());
		}
		verify(log).warn(REVERT_ERROR_MESSAGE);
	}

	@Test
	public void closeSCMExceptionOccurred() throws Exception {
		final SCMException expected = new SCMException("any");
		doThrow(expected).when(repository).revertChanges(set);
		try {
			set.close();
			fail("Exception expected!");
		} catch (final Exception e) {
			assertSame(expected, e.getCause());
		}
		verify(log, never()).warn(REVERT_ERROR_MESSAGE);
	}
}
