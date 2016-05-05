package com.github.danielflower.mavenplugins.release.pom;

import static com.github.danielflower.mavenplugins.release.pom.UpdateProcessor.DEPENDENCY_ERROR_INTRO;
import static com.github.danielflower.mavenplugins.release.pom.UpdateProcessor.DEPENDENCY_ERROR_SUMMARY;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.reactor.Reactor;

/**
 * @author rolandhauser
 *
 */
public class UpdateProcessorTest {
	private static final String ANY_ARTIFACT_ID = "anyArtifactId";
	private static final String ANY_VERSION = "anyVersion";
	private static final String ANY_ERROR = "anyError";
	private static final File ANY_POM = new File("anyPom");
	private final Log log = mock(Log.class);
	private final Reactor reactor = mock(Reactor.class);
	private final ReleasableModule module = mock(ReleasableModule.class);
	private final ContextFactory contextFactory = mock(ContextFactory.class);
	private final PomWriterFactory writerFactory = mock(PomWriterFactory.class);
	private final PomWriter writer = mock(PomWriter.class);
	private final Context context = mock(Context.class);
	private final Command command = mock(Command.class);
	private final List<Command> commands = asList(command);
	private final MavenProject project = mock(MavenProject.class);
	private final Model originalModel = mock(Model.class);
	private final UpdateProcessor processor = new UpdateProcessor(contextFactory, writerFactory, commands);

	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws ValidationException {
		// Setup context factory
		when(contextFactory.newContext(log, reactor, project, ANY_VERSION)).thenReturn(context);

		// Setup context
		when(context.getErrors()).thenReturn(Collections.<String> emptyList());

		// Setup writer factory
		when(writerFactory.newWriter(log)).thenReturn(writer);

		// Setup writer
		when(writer.writePoms()).thenReturn(Arrays.asList(ANY_POM));

		// Setup reactor
		final Iterator<ReleasableModule> it = mock(Iterator.class);
		when(it.hasNext()).thenReturn(true).thenReturn(false);
		when(it.next()).thenReturn(module).thenThrow(NoSuchElementException.class);
		when(reactor.iterator()).thenReturn(it);

		// Setup module
		when(module.willBeReleased()).thenReturn(true);
		when(module.getProject()).thenReturn(project);
		when(module.getArtifactId()).thenReturn(ANY_ARTIFACT_ID);
		when(module.getNewVersion()).thenReturn(ANY_VERSION);

		// Setup project
		when(project.getOriginalModel()).thenReturn(originalModel);
	}

	@Test
	public void updatePomsCompletedSuccessfully() throws Exception {
		final List<File> updatedPoms = processor.updatePoms(log, reactor);
		assertEquals(1, updatedPoms.size());
		assertSame(ANY_POM, updatedPoms.get(0));

		final InOrder order = inOrder(originalModel, command, log, writer);
		order.verify(log).info("Going to release anyArtifactId anyVersion");
		order.verify(originalModel).setVersion(ANY_VERSION);
		order.verify(command).alterModel(context);
		order.verify(writer).addProject(project);
	}

	@Test
	public void updatePomsDependencyErrorsOccurred() throws Exception {
		when(context.getErrors()).thenReturn(asList(ANY_ERROR));
		try {
			processor.updatePoms(log, reactor);
			fail("Exception expected");
		} catch (final ValidationException e) {
			assertEquals(DEPENDENCY_ERROR_SUMMARY, e.getMessage());
			final List<String> msgs = e.getMessages();
			assertEquals(DEPENDENCY_ERROR_SUMMARY, msgs.get(0));
			assertEquals(DEPENDENCY_ERROR_INTRO, msgs.get(1));
			assertEquals(" * anyError", msgs.get(2));
		}
	}

	/**
	 * TODO: Check todo-comment in
	 * {@link UpdateProcessor#updatePoms(Log, Reactor)} and rewrite this test if
	 * necessary
	 */
	@Test
	public void updatePomsModuleWillNotBeReleased() throws Exception {
		when(module.willBeReleased()).thenReturn(false);
		final List<File> updatedPoms = processor.updatePoms(log, reactor);
		assertEquals(1, updatedPoms.size());
		assertSame(ANY_POM, updatedPoms.get(0));

		final InOrder order = inOrder(originalModel, command, log, writer);

		// This is the only difference between a "normal" run and a run when
		// module#willBeReleased() returns false; see
		// updatePomsCompletedSuccessfully
		order.verify(log, Mockito.never()).info("Going to release anyArtifactId anyVersion");

		order.verify(originalModel).setVersion(ANY_VERSION);
		order.verify(command).alterModel(context);
		order.verify(writer).addProject(project);
	}
}
