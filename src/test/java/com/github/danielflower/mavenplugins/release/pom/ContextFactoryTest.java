package com.github.danielflower.mavenplugins.release.pom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.reactor.ReleasableModule;

public class ContextFactoryTest {
	private static final String ANY_GROUP_ID = "anyGroupId";
	private static final String ANY_ARTIFACT_ID = "anyArtifactId";
	private static final String TEST_STRING = "test";
	private final Reactor reactor = mock(Reactor.class);
	private final MavenProject project = mock(MavenProject.class);
	private Context context;

	@Before
	public void setup() {
		context = new ContextFactory().newContext(reactor, project, false);
	}

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
	public void getVersionToDependOn() throws Exception {
		final ReleasableModule module = mock(ReleasableModule.class);
		when(module.getVersionToDependOn()).thenReturn(TEST_STRING);
		when(reactor.find(ANY_GROUP_ID, ANY_ARTIFACT_ID)).thenReturn(module);
		assertEquals(module, context.getVersionToDependOn(ANY_GROUP_ID, ANY_ARTIFACT_ID));
	}

}
