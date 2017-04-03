package com.github.danielflower.mavenplugins.release;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.apache.maven.project.MavenProject;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.versioning.ImmutableFixVersion;

public class ReleasableModuleTest {
    @Test
    public void getsTheTagFromTheArtifactAndVersion() throws Exception {
        final MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifactId("my-artifact");
        mavenProject.setGroupId("my-group");
        final ImmutableReleasableModule.Builder builder = ImmutableReleasableModule.builder();
        builder.isToBeReleased(false);
        builder.relativePathToModule("..");
        ReleasableModule module = builder.project(mavenProject).version(
            ImmutableFixVersion.builder().majorVersion(1).minorVersion(0).build()).build();
        assertThat(module.getProject().getArtifactId() + "-" + module.getVersion().toString(),
                   equalTo("my-artifact-1.0"));
    }
}
