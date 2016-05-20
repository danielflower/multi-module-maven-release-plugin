package com.github.danielflower.mavenplugins.release.version;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.scm.ProposedTag;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

public class BuildNumberFinderTest {
	private static final String ANY_ARTIFACT_ID = "anyArtifactId";
	private static final String ANY_REMOTE_URL = "anyRemoteUrl";
	private static final String ANY_BUSINESS_VERSION = "anyBusinessVersion";
	private final SCMRepository repository = mock(SCMRepository.class);
	private final MavenProject project = mock(MavenProject.class);
	private final ProposedTag tag = mock(ProposedTag.class);
	private final List<ProposedTag> tags = asList(tag);
	private final List<Long> remoteBuildNumbers = asList(9l, 2l, 7l);
	private final BuildNumberFinder finder = new BuildNumberFinder();

	@Before
	public void setup() {
		when(project.getArtifactId()).thenReturn(ANY_ARTIFACT_ID);
		when(tag.buildNumber()).thenReturn(6l);
		finder.setRepository(repository);
	}

	@Test
	public void findBuildNumberNoTagsAndNoRemoteBuildNumbersFound() throws Exception {
		when(repository.tagsForVersion(ANY_ARTIFACT_ID, ANY_BUSINESS_VERSION))
				.thenReturn(Collections.<ProposedTag> emptyList());
		when(repository.getRemoteBuildNumbers(ANY_REMOTE_URL, ANY_ARTIFACT_ID, ANY_BUSINESS_VERSION))
				.thenReturn(Collections.<Long> emptyList());

		assertEquals(0l, finder.findBuildNumber(project, ANY_REMOTE_URL, ANY_BUSINESS_VERSION));
	}

	@Test
	public void findBuildNumberWithTagsAndRemoteBuildNumbers() throws Exception {
		when(repository.getRemoteBuildNumbers(ANY_REMOTE_URL, ANY_ARTIFACT_ID, ANY_BUSINESS_VERSION))
				.thenReturn(remoteBuildNumbers);
		when(repository.tagsForVersion(ANY_ARTIFACT_ID, ANY_BUSINESS_VERSION)).thenReturn(tags);

		// Must be the last version incremented by 1
		assertEquals(10l, finder.findBuildNumber(project, ANY_REMOTE_URL, ANY_BUSINESS_VERSION));
	}

	@Test
	public void findBuildNumberWithTags() throws Exception {
		when(repository.tagsForVersion(ANY_ARTIFACT_ID, ANY_BUSINESS_VERSION)).thenReturn(tags);

		// Must be the last version incremented by 1
		assertEquals(7l, finder.findBuildNumber(project, ANY_REMOTE_URL, ANY_BUSINESS_VERSION));
	}

	@Test
	public void findBuildNumberWithRemoteBuildNumbers() throws Exception {
		when(repository.getRemoteBuildNumbers(ANY_REMOTE_URL, ANY_ARTIFACT_ID, ANY_BUSINESS_VERSION))
				.thenReturn(remoteBuildNumbers);

		// Must be the last version incremented by 1
		assertEquals(10l, finder.findBuildNumber(project, ANY_REMOTE_URL, ANY_BUSINESS_VERSION));
	}
}
