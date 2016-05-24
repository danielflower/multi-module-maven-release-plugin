package com.github.danielflower.mavenplugins.release.pom;

import static com.github.danielflower.mavenplugins.release.pom.PomWriter.EXCEPTION_MESSAGE;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.github.danielflower.mavenplugins.release.scm.SCMException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

public class PomWriterFactoryTest {
	private final class HasOneChangedPomFile implements ArgumentMatcher<List<File>> {
		@SuppressWarnings("unchecked")
		@Override
		public boolean matches(final Object item) {
			final List<File> changedFiles = (List<File>) item;
			return changedFiles.size() == 1 && changedFiles.contains(testFile);
		}

		@Override
		public String toString() {
			return "List with 1 changed file";
		}
	}

	private static final String TEST_LINE = "This is a test";
	private final SCMRepository repository = mock(SCMRepository.class);
	private final MavenXpp3Writer writer = mock(MavenXpp3Writer.class);
	private final MavenXpp3WriterFactory writerFactory = mock(MavenXpp3WriterFactory.class);
	private final Log log = mock(Log.class);
	private final MavenProject project = mock(MavenProject.class);
	private final Model originalModel = mock(Model.class);
	private PomWriter pomWriter;
	private File testFile;

	@Before
	public void setup() throws IOException {
		testFile = new File("target/pomWriterTest").getCanonicalFile();
		when(writerFactory.newWriter()).thenReturn(writer);
		when(project.getOriginalModel()).thenReturn(originalModel);
		when(project.getFile()).thenReturn(testFile);
		final PomWriterFactory factory = new PomWriterFactory();
		factory.setLog(log);
		factory.setMavenXpp3WriterFactory(writerFactory);
		factory.setRepository(repository);
		pomWriter = factory.newWriter();
		pomWriter.addProject(project);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void pomsSuccessfullyWritten() throws Exception {
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(final InvocationOnMock invocation) throws Throwable {
				invocation.getArgumentAt(0, FileWriter.class).write(TEST_LINE);
				return null;
			}
		}).when(writer).write((Writer) Mockito.notNull(), Mockito.same(originalModel));

		final ChangeSet changedFiles = pomWriter.writePoms();
		try (final Scanner sc = new Scanner(testFile)) {
			assertEquals(TEST_LINE, sc.nextLine());
		}
		final Iterator<File> it = changedFiles.iterator();
		assertTrue(it.hasNext());
		assertEquals(testFile, it.next());
		assertFalse(it.hasNext());
		verify(repository, never()).revertChanges(Mockito.anyList());
	}

	@Test
	public void ioExceptionOccurredWhileWriting() throws Exception {
		final IOException expected = new IOException();
		doThrow(expected).when(writer).write((Writer) Mockito.notNull(), Mockito.same(originalModel));
		try {
			pomWriter.writePoms();
			fail("Exception expected");
		} catch (final POMUpdateException e) {
			assertSame(expected, e.getCause());
			assertEquals(EXCEPTION_MESSAGE, e.getMessage());
		}
		verify(repository).revertChanges(Mockito.argThat(new HasOneChangedPomFile()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void ioExceptionOccurredWhileReverting() throws Exception {
		final SCMException revertException = new SCMException("any");
		doThrow(revertException).when(repository).revertChanges(Mockito.anyList());

		final IOException expected = new IOException();
		doThrow(expected).when(writer).write((Writer) Mockito.notNull(), Mockito.same(originalModel));
		try {
			pomWriter.writePoms();
			fail("Exception expected");
		} catch (final POMUpdateException e) {
			assertSame(expected, e.getCause());
			assertEquals(EXCEPTION_MESSAGE, e.getMessage());
		}

		final InOrder order = inOrder(log, repository);
		order.verify(repository).revertChanges(Mockito.argThat(new HasOneChangedPomFile()));
		order.verify(log).error(format("Reverting changed POMs [%s] failed!", testFile), revertException);
	}
}
