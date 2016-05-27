package com.github.danielflower.mavenplugins.release.version;

import static com.github.danielflower.mavenplugins.release.version.DefaultVersionBuilder.SNAPSHOT_EXTENSION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DefaultVersionBuilderFactoryTest {
	private static final String BUSINESS_VERSION = "1.0";
	private static final String DEVELOPMENT_VERSION = BUSINESS_VERSION + SNAPSHOT_EXTENSION;
	private static final Long BUILD_NUMBER = 9L;
	private static final String CHANGED_DEPENDENCY = "changedDependency";
	private static final String RELATIVE_PATH_TO_MODULE = "relativePathToModule";
	private static final String EQUIVALENT_VERSION = "equivalentVersion";
	private static final String ANY_REMOTE_URL = "anyRemoteUrl";
	private final BuildNumberFinder finder = mock(BuildNumberFinder.class);
	private final ChangeDetectorFactory detectorFactory = mock(ChangeDetectorFactory.class);
	private final ChangeDetector detector = mock(ChangeDetector.class);
	private final MavenProject project = mock(MavenProject.class);
	private final VersionBuilder builder = new DefaultVersionBuilder(finder, detectorFactory);

	@Before
	public void setup() throws VersionException {
		when(detectorFactory.newDetector()).thenReturn(detector);
		when(detector.setProject(project)).thenReturn(detector);
		when(detector.setBuildNumber(BUILD_NUMBER)).thenReturn(detector);
		when(detector.setChangedDependency(CHANGED_DEPENDENCY)).thenReturn(detector);
		when(detector.setRelativePathToModule(RELATIVE_PATH_TO_MODULE)).thenReturn(detector);
		when(detector.setBusinessVersion(BUSINESS_VERSION)).thenReturn(detector);
		when(detector.equivalentVersionOrNull()).thenReturn(EQUIVALENT_VERSION);

		when(project.getVersion()).thenReturn(DEVELOPMENT_VERSION);
		builder.setProject(project);
		builder.setChangedDependency(CHANGED_DEPENDENCY);
		builder.setRelativePath(RELATIVE_PATH_TO_MODULE);
		builder.setRemoteUrl(ANY_REMOTE_URL);
	}

	@Test(expected = VersionException.class)
	public void buildWhenUseLastDigitAsBuildNumberAndBuildNumberAreSet() throws VersionException {
		builder.setUseLastDigitAsBuildNumber(true);
		builder.setBuildNumber(9l);
		builder.build();
	}

	@Test
	public void verifyEvaluateBuildNumber() throws VersionException {
		when(finder.findBuildNumber(project, ANY_REMOTE_URL, BUSINESS_VERSION)).thenReturn(10l);
		when(detector.setBuildNumber(10l)).thenReturn(detector);
		final Version version = builder.build();
		assertEquals(10l, version.getBuildNumber());
	}

	@Ignore
	@Test
	public void verifyEvaluateBuildNumberUseLastDigitAsBuildNumber() throws VersionException {
		builder.setUseLastDigitAsBuildNumber(true);
		when(project.getVersion()).thenReturn("1.2.3-SNAPSHOT");
		when(detector.setBuildNumber(3l)).thenReturn(detector);
		final Version version = builder.build();
		assertEquals(3, version.getBuildNumber());
	}
}
