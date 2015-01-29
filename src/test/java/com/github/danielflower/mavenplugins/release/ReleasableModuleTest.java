package com.github.danielflower.mavenplugins.release;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ReleasableModuleBuilder.aModule;

public class ReleasableModuleTest {
    @Test
    public void getsTheTagFromTheArtifactAndVersion() throws Exception {
        ReleasableModule module = aModule()
            .withArtifactId("my-artifact")
            .withSnapshotVersion("1.0-SNAPSHOT")
            .withBuildNumber("123")
            .build();
        assertThat(module.getTagName(), equalTo("my-artifact-1.0.123"));
    }
}
