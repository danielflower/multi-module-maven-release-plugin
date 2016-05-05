package com.github.danielflower.mavenplugins.release.log;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

public class LogProviderTest {
	private static final String TEST_STRING = "This is a test";
	private final Log log = mock(Log.class);
	private final LogInvocationHandler handler = new LogInvocationHandler();
	private final LogProvider provider = new LogProvider(handler);

	@Test(expected = IllegalStateException.class)
	public void uninstallizedAccess() {
		provider.get().info(TEST_STRING);
	}

	@Test
	public void verifyCallDelegate() {
		handler.setLog(log);
		provider.get().info(TEST_STRING);
		verify(log).info(TEST_STRING);
	}
}
