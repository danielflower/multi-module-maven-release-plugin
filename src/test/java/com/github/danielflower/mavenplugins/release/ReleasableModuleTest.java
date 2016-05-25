package com.github.danielflower.mavenplugins.release;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertSame;
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
		assertThat(module.getTagName(), equalTo("my-artifact-1.0.123"));
	}

	@Test
	public void aReleaseableModuleCanBeCreatedFromAnUnreleasableOne() {
		final MavenProject project = new MavenProject();
		project.setArtifactId("some-arty");
		project.setGroupId("some-group");
		final Version version = mock(Version.class);
		when(version.getBuildNumber()).thenReturn(12l);
		when(version.getBusinessVersion()).thenReturn("1.2.3");
		when(version.getDevelopmentVersion()).thenReturn("1.2.3-SNAPSHOT");
		final ReleasableModule first = new ReleasableModule(project, version, "1.2.3.11", "somewhere");
		assertThat(first.willBeReleased(), is(false));

		when(version.getReleaseVersion()).thenReturn("1.2.3.12");
		final ReleasableModule changed = first.createReleasableVersion();

		assertSame(version, changed.getVersion());

		assertThat(changed.getArtifactId(), equalTo("some-arty"));
		assertThat(changed.getGroupId(), equalTo("some-group"));
		assertThat(changed.getProject(), is(project));
		assertThat(changed.getRelativePathToModule(), equalTo("somewhere"));
		assertThat(changed.willBeReleased(), is(true));
	}
}
