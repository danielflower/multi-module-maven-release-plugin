package com.github.danielflower.mavenplugins.release;

import static com.github.danielflower.mavenplugins.release.TestUtils.fixVersion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;

import org.apache.maven.project.MavenProject;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.versioning.ImmutableModuleVersion;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableQualifiedArtifact;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseDateSingleton;

public class ReleasableModuleTest {
    @Test
    public void getsTheTagFromTheArtifactAndVersion() throws Exception {
        final MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifactId("my-artifact");
        mavenProject.setGroupId("my-group");
        final ImmutableReleasableModule.Builder builder = ImmutableReleasableModule.builder();
        builder.isToBeReleased(false);
        builder.relativePathToModule("..");
        final ImmutableModuleVersion.Builder moduleBuilder = ImmutableModuleVersion.builder();
        moduleBuilder.version(fixVersion(1, 0));
        moduleBuilder.releaseTag(ReleaseDateSingleton.getInstance().tagName());
        moduleBuilder.releaseDate(ReleaseDateSingleton.getInstance().releaseDate());
        moduleBuilder.artifact(ImmutableQualifiedArtifact.builder().groupId("my-group").artifactId("my-artifact").build
                                                                                                                    ());
        builder.immutableModule(moduleBuilder.build());

        ReleasableModule module = builder.project(mavenProject).immutableModule(moduleBuilder.build()).build();
        assertThat(module.getProject().getArtifactId() + "-" + module.getImmutableModule().getVersion().toString(),
                   startsWith("my-artifact-1.0"));
    }
}
