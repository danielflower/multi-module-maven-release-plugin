package com.github.danielflower.mavenplugins.release.log;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;

public class LogProviderTest {
	private static final String TEST_STRING = "This is a test";
	private final Throwable throwable = new Throwable();
	private final Log log = mock(Log.class);
	private final LogProvider provider = new LogProvider();

	@Before
	public void setup() {
		provider.setLog(log);
	}

	@Test(expected = IllegalStateException.class)
	public void illegalStateWhenLogDelegateNotSet() {
		provider.setLog(null);
		provider.isDebugEnabled();
	}

	@Test
	public void isDebugEnabled() {
		when(log.isDebugEnabled()).thenReturn(true);
		assertTrue(provider.isDebugEnabled());
	}

	@Test
	public void debug() {
		provider.debug(TEST_STRING);
		verify(log).debug(TEST_STRING);
	}

	@Test
	public void debugWithStringAndThrowable() {
		provider.debug(TEST_STRING, throwable);
		verify(log).debug(TEST_STRING, throwable);
	}

	@Test
	public void debugWithThrowable() {
		provider.debug(throwable);
		verify(log).debug(throwable);
	}

	@Test
	public void isInfoEnabled() {
		when(log.isInfoEnabled()).thenReturn(true);
		assertTrue(provider.isInfoEnabled());
	}

	@Test
	public void info() {
		provider.info(TEST_STRING);
		verify(log).info(TEST_STRING);
	}

	@Test
	public void infoWithStringAndThrowable() {
		provider.info(TEST_STRING, throwable);
		verify(log).info(TEST_STRING, throwable);
	}

	@Test
	public void infoWithThrowable() {
		provider.info(throwable);
		verify(log).info(throwable);
	}

	@Test
	public void isWarnEnabled() {
		when(log.isWarnEnabled()).thenReturn(true);
		assertTrue(provider.isWarnEnabled());
	}

	@Test
	public void warn() {
		provider.warn(TEST_STRING);
		verify(log).warn(TEST_STRING);
	}

	@Test
	public void warnWithStringAndThrowable() {
		provider.warn(TEST_STRING, throwable);
		verify(log).warn(TEST_STRING, throwable);
	}

	@Test
	public void warnWithThrowable() {
		provider.warn(throwable);
		verify(log).warn(throwable);
	}

	@Test
	public void isErrorEnabled() {
		when(log.isErrorEnabled()).thenReturn(true);
		assertTrue(provider.isErrorEnabled());
	}

	@Test
	public void error() {
		provider.error(TEST_STRING);
		verify(log).error(TEST_STRING);
	}

	@Test
	public void errorWithStringAndThrowable() {
		provider.error(TEST_STRING, throwable);
		verify(log).error(TEST_STRING, throwable);
	}

	@Test
	public void errorWithThrowable() {
		provider.error(TEST_STRING, throwable);
		verify(log).error(throwable);
	}
}
