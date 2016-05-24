package com.github.danielflower.mavenplugins.release.pom;

import static com.github.danielflower.mavenplugins.release.pom.UpdateProcessor.DEPENDENCY_ERROR_INTRO;
import static com.github.danielflower.mavenplugins.release.pom.UpdateProcessor.DEPENDENCY_ERROR_SUMMARY;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
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

import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.reactor.ReleasableModule;

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
	private final ChangeSet changeSet = mock(ChangeSet.class);
	private final Model originalModel = mock(Model.class);
	private UpdateProcessor processor;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws POMUpdateException {
		// Setup context factory
		when(contextFactory.newReleaseContext(reactor, project)).thenReturn(context);

		// Setup context
		when(context.getErrors()).thenReturn(Collections.<String> emptyList());

		// Setup writer factory
		when(writerFactory.newWriter()).thenReturn(writer);

		// Setup writer
		when(writer.writePoms()).thenReturn(changeSet);
		when(changeSet.iterator()).thenReturn(Arrays.asList(ANY_POM).iterator());

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

		processor = new UpdateProcessor();
		processor.setCommands(commands);
		processor.setContextFactory(contextFactory);
		processor.setLog(log);
		processor.setPomWriterFactory(writerFactory);
	}

	@Test
	public void updatePomsCompletedSuccessfully() throws Exception {
		final ChangeSet updatedPoms = processor.updatePoms(reactor);
		final Iterator<File> it = updatedPoms.iterator();
		assertTrue(it.hasNext());
		assertSame(ANY_POM, it.next());
		assertFalse(it.hasNext());

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
			processor.updatePoms(reactor);
			fail("Exception expected");
		} catch (final POMUpdateException e) {
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
		final ChangeSet updatedPoms = processor.updatePoms(reactor);
		final Iterator<File> it = updatedPoms.iterator();
		assertTrue(it.hasNext());
		assertSame(ANY_POM, it.next());
		assertFalse(it.hasNext());

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
