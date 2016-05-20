package com.github.danielflower.mavenplugins.release.version;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

/**
 * @author rolandhauser
 *
 */
public class DefaultVersionTest {
	private static final String ANY_REMOTE_URL = "anyRemoteUrl";
	private static final String ARTIFACT_ID = "anyArtifactId";
	private final SCMRepository gitRepo = mock(SCMRepository.class);
	private final Git git = mock(Git.class);
	private final BuildNumberFinder finder = new BuildNumberFinder();
	private final ListTagCommand cmd = mock(ListTagCommand.class);
	private final MavenProject project = mock(MavenProject.class);

	@Before
	public void setup() throws GitAPIException {
		when(project.getArtifactId()).thenReturn(ARTIFACT_ID);
		final List<Ref> ref = Collections.emptyList();
		when(cmd.call()).thenReturn(ref);
		when(git.tagList()).thenReturn(cmd);
	}

	@Test
	public void removesTheSnapshotAndSticksTheBuildNumberOnTheEnd() throws Exception {
		final Version version = new DefaultVersion(ARTIFACT_ID, "1.0", 123L);
		assertEquals(version.releaseVersion(), "1.0.123");
	}

	@Test
	public void ifTheBuildNumberIsNullAndThePreviousBuildNumbersIsEmptyListThenZeroIsUsed() throws Exception {
		final Version version = new DefaultVersion(ARTIFACT_ID, "1.0", 0l);
		assertEquals("1.0.0", version.releaseVersion());
	}
}
