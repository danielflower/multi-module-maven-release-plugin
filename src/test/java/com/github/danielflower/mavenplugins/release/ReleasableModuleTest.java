package com.github.danielflower.mavenplugins.release;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;

public class ReleasableModuleTest {
    @Test
    public void getsTheTagFromTheArtifactAndVersion() throws Exception {
        ReleasableModule module = aModule()
            .artifactId("my-artifact")
            .newVersion("1.0-SNAPSHOT")
            .build();
        assertThat(module.getArtifactId() + "-" + module.getNewVersion(), equalTo("my-artifact-1.0.123"));
    }

    private ImmutableReleasableModule.Builder aModule() {
        return ImmutableReleasableModule.builder();
    }

}
