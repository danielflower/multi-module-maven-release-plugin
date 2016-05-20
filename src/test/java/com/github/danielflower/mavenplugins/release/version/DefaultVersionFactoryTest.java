package com.github.danielflower.mavenplugins.release.version;

import static com.github.danielflower.mavenplugins.release.version.DefaultVersionFactory.SNAPSHOT_EXTENSION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

public class DefaultVersionFactoryTest {
	private static final String VERSION = "1.0";
	private static final String SNAPSHOT_VERSION = VERSION + SNAPSHOT_EXTENSION;
	private static final String ANY_REMOTE_URL = "anyRemoteUrl";
	private final SCMRepository repository = mock(SCMRepository.class);
	private final BuildNumberFinder finder = mock(BuildNumberFinder.class);
	private final MavenProject project = mock(MavenProject.class);
	private final DefaultVersionFactory factory = new DefaultVersionFactory();

	@Before
	public void setup() {
		when(project.getVersion()).thenReturn(SNAPSHOT_VERSION);
		factory.setRepository(repository);
		factory.setFinder(finder);
	}

	@Test
	public void newVersionWithLastDigitAsBuildNumberNoExplicitBuildNumberSpecified() throws Exception {
		when(project.getVersion()).thenReturn("1.0.1-SNAPSHOT");
		final Version version = factory.newVersion(project, true, null, ANY_REMOTE_URL);
		assertEquals(VERSION, version.getBusinessVersion());
		assertEquals("1.0.1-SNAPSHOT", version.getDevelopmentVersion());
		assertEquals("1.0.1", version.getReleaseVersion());
		assertEquals(1, version.getBuildNumber());
	}

	@Test
	public void newVersionWithSpecifiedBuildNumber() throws Exception {
		// Pass 'true' for useLastDigitAsBuildNumber to insure that in any
		// case the build number specified is used.
		final Version version = factory.newVersion(project, true, 9l, ANY_REMOTE_URL);
		assertEquals(VERSION, version.getBusinessVersion());
		assertEquals("1.0-SNAPSHOT", version.getDevelopmentVersion());
		assertEquals("1.0.9", version.getReleaseVersion());
		assertEquals(9, version.getBuildNumber());
	}

	@Test
	public void newVersionWithImplicitBuildNumber() throws Exception {
		when(finder.findBuildNumber(project, ANY_REMOTE_URL, VERSION)).thenReturn(10l);
		final Version version = factory.newVersion(project, false, null, ANY_REMOTE_URL);
		assertEquals(VERSION, version.getBusinessVersion());
		assertEquals("1.0-SNAPSHOT", version.getDevelopmentVersion());
		assertEquals("1.0.10", version.getReleaseVersion());
		assertEquals(10, version.getBuildNumber());
	}
}
