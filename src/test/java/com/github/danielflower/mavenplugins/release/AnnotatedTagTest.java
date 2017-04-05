package com.github.danielflower.mavenplugins.release;

import e2e.ProjectType;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.Rule;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.versioning.ModuleVersion;
import com.google.gson.JsonSyntaxException;

public class AnnotatedTagTest {

    @Rule
    public TestProject project = new TestProject(ProjectType.SINGLE);

    @Test
    public void gettersReturnValuesPassedIn() throws Exception {
        AnnotatedTag tag = new AnnotatedTag(null, "my-name", TestUtils.releaseInfo(4L, 2134L, "test", "my-name"));
        assertThat(tag.name(), equalTo("my-name"));
        final ModuleVersion moduleVersion = tag.getReleaseInfo().versionForArtifact(TestUtils.artifactIdForModule
                                                                                                ("my-name")).get();
        assertThat(moduleVersion.getVersion().getMajorVersion(), equalTo(4L));
        assertThat(moduleVersion.getVersion().getMinorVersion(), equalTo(2134L));
    }

    @Test
    public void aTagCanBeCreatedFromAGitTag() throws GitAPIException, IOException {
        AnnotatedTag tag = new AnnotatedTag(null, "my-name", TestUtils.releaseInfo(4L, 2134L, "test", "my-name"));
        tag.saveAtHEAD(project.local);

        Ref ref = project.local.tagList().call().get(0);
        AnnotatedTag inflatedTag = AnnotatedTag.fromRef(project.local.getRepository(), ref);
        final ModuleVersion moduleVersion = tag.getReleaseInfo().versionForArtifact(TestUtils.artifactIdForModule
                                                                                                  ("my-name")).get();
        assertThat(moduleVersion.getVersion().getMajorVersion(), equalTo(4L));
        assertThat(moduleVersion.getVersion().getMinorVersion(), equalTo(2134L));
    }

    @Test(expected = JsonSyntaxException.class)
    public void ifATagIsSavedWithoutJsonThenAnExceptionIsThrown() throws GitAPIException, IOException {
        project.local.tag().setName("my-name-1.0.2").setAnnotated(true).setMessage("This is not json").call();

        Ref ref = project.local.tagList().call().get(0);
        AnnotatedTag inflatedTag = AnnotatedTag.fromRef(project.local.getRepository(), ref);
    }

}
