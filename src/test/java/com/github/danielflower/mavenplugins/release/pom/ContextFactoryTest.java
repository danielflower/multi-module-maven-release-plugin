package com.github.danielflower.mavenplugins.release.pom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;

public class ContextFactoryTest {
	private static final String ANY_VERSION = "anyVersion";
	private static final String TEST_STRING = "test";
	private final Reactor reactor = mock(Reactor.class);
	private final MavenProject project = mock(MavenProject.class);
	private final Context context = new ContextFactory().newContext(reactor, project, ANY_VERSION);

	@Test
	public void verfiyAddGetError() {
		context.addError("%s 1", TEST_STRING);
		context.addError("%s 2", TEST_STRING);
		final List<String> errors = context.getErrors();
		assertEquals(2, errors.size());
		assertEquals("test 1", errors.get(0));
		assertEquals("test 2", errors.get(1));
	}

	@Test
	public void getProject() {
		assertSame(project, context.getProject());
	}

	@Test
	public void getReactor() {
		assertSame(reactor, context.getReactor());
	}

	@Test
	public void getNewVersion() {
		assertEquals(ANY_VERSION, context.getNewVersion());
	}
}
