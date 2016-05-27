package com.github.danielflower.mavenplugins.release;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static scaffolding.ReleasableModuleBuilder.aModule;

import org.apache.maven.project.MavenProject;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.reactor.ReleasableModule;
import com.github.danielflower.mavenplugins.release.version.Version;

public class ReleasableModuleTest {

	@Test
	public void getsTheTagFromTheArtifactAndVersion() throws Exception {
		final ReleasableModule module = aModule().withArtifactId("my-artifact").withSnapshotVersion("1.0-SNAPSHOT")
				.withBuildNumber(123).build();
		assertEquals("my-artifact-1.0.123", module.getTagName());
	}

	@Test
	public void aReleaseableModuleCanBeCreatedFromAnUnreleasableOne() {
		final MavenProject project = new MavenProject();
		project.setArtifactId("some-arty");
		project.setGroupId("some-group");
		final Version version = mock(Version.class);
		when(version.getBuildNumber()).thenReturn(12l);
		when(version.getBusinessVersion()).thenReturn("1.2.3");
		when(version.getEquivalentVersionOrNull()).thenReturn("1.2.3.11");
		final ReleasableModule first = new ReleasableModule(project, version, "somewhere");
		assertFalse(first.willBeReleased());

		when(version.getReleaseVersion()).thenReturn("1.2.3.12");
		when(version.getEquivalentVersionOrNull()).thenReturn(null);
		first.getVersion().makeReleaseable();

		assertSame(version, first.getVersion());

		assertEquals("some-arty", first.getArtifactId());
		assertEquals("some-group", first.getGroupId());
		assertSame(project, first.getProject());
		assertEquals("somewhere", first.getRelativePathToModule());
		assertTrue(first.willBeReleased());
	}
}
