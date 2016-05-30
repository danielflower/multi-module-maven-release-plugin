package com.github.danielflower.mavenplugins.release.scm;

import static com.github.danielflower.mavenplugins.release.scm.AnnotatedTagFinderTest.saveFileInModule;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Test;

import scaffolding.TestProject;

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

	@Test
	public void canDetectIfFilesHaveBeenChangedForAModuleSinceSomeSpecificTag() throws Exception {
		final TestProject project = TestProject.independentVersionsProject();

		final ProposedTag tag1 = saveFileInModule(project, "console-app", "1.2", 3);
		final ProposedTag tag2 = saveFileInModule(project, "core-utils", "2", 0);
		final ProposedTag tag3 = saveFileInModule(project, "console-app", "1.2", 4);

		final GitRepository detector = new GitRepository();
		when(gitFactory.newGit()).thenReturn(project.local);
		detector.setGitFactory(gitFactory);

		assertThat(detector.hasChangedSince("core-utils", noChildModules(), asList(tag2)), is(false));
		assertThat(detector.hasChangedSince("console-app", noChildModules(), asList(tag2)), is(true));
		assertThat(detector.hasChangedSince("console-app", noChildModules(), asList(tag3)), is(false));
	}

	@Test
	public void canDetectThingsInTheRoot() throws Exception {
		final TestProject simple = TestProject.singleModuleProject();
		final ProposedTag tag1 = saveFileInModule(simple, ".", "1.0", 1);
		simple.commitRandomFile(".");
		final GitRepository detector = new GitRepository();
		when(gitFactory.newGit()).thenReturn(simple.local);
		detector.setGitFactory(gitFactory);
		assertThat(detector.hasChangedSince(".", noChildModules(), asList(tag1)), is(true));

		final ProposedTag tag2 = saveFileInModule(simple, ".", "1.0", 2);
		assertThat(detector.hasChangedSince(".", noChildModules(), asList(tag2)), is(false));
	}

	@Test
	public void canDetectChangesAfterTheLastTag() throws Exception {
		final TestProject project = TestProject.independentVersionsProject();

		saveFileInModule(project, "console-app", "1.2", 3);
		saveFileInModule(project, "core-utils", "2", 0);
		final ProposedTag tag3 = saveFileInModule(project, "console-app", "1.2", 4);
		project.commitRandomFile("console-app");

		final GitRepository detector = new GitRepository();
		when(gitFactory.newGit()).thenReturn(project.local);
		detector.setGitFactory(gitFactory);
		assertThat(detector.hasChangedSince("console-app", noChildModules(), asList(tag3)), is(true));
	}

	@Test
	public void canIgnoreModuleFolders() throws Exception {
		final TestProject project = TestProject.independentVersionsProject();

		saveFileInModule(project, "console-app", "1.2", 3);
		saveFileInModule(project, "core-utils", "2", 0);
		final ProposedTag tag3 = saveFileInModule(project, "console-app", "1.2", 4);
		project.commitRandomFile("console-app");

		final GitRepository detector = new GitRepository();
		when(gitFactory.newGit()).thenReturn(project.local);
		detector.setGitFactory(gitFactory);
		assertThat(detector.hasChangedSince("console-app", asList("console-app"), asList(tag3)), is(false));
	}

	private static java.util.List<String> noChildModules() {
		return new ArrayList<String>();
	}
}
