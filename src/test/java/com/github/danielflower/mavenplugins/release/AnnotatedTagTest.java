package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;
import scaffolding.TestProject;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotatedTagTest {
    @Test
    public void gettersReturnValuesPassedIn() throws Exception {
        // yep, testing getters... but only because it isn't a simple POJO
        AnnotatedTag tag = AnnotatedTag.create("my-name", "the-version", 2134);
        assertThat(tag.name(), equalTo("my-name"));
        assertThat(tag.version(), equalTo("the-version"));
        assertThat(tag.buildNumber(), equalTo(2134L));
    }

    @Test
    public void aTagCanBeCreatedFromAGitTag() throws GitAPIException, IOException {
        TestProject project = TestProject.singleModuleProject();
        AnnotatedTag tag = AnnotatedTag.create("my-name", "the-version", 2134);
        tag.saveAtHEAD(project.local);

        Ref ref = project.local.tagList().call().get(0);
        AnnotatedTag inflatedTag = AnnotatedTag.fromRef(project.local.getRepository(), ref);
        assertThat(inflatedTag.name(), equalTo("my-name"));
        assertThat(inflatedTag.version(), equalTo("the-version"));
        assertThat(inflatedTag.buildNumber(), equalTo(2134L));
    }

    @Test
    public void ifATagIsSavedWithoutJsonThenTheVersionIsSetTo0Dot0() throws GitAPIException, IOException {
        TestProject project = TestProject.singleModuleProject();
        project.local.tag().setName("my-name-1.0.2").setAnnotated(true).setMessage("This is not json").call();

        Ref ref = project.local.tagList().call().get(0);
        AnnotatedTag inflatedTag = AnnotatedTag.fromRef(project.local.getRepository(), ref);
        assertThat(inflatedTag.name(), equalTo("my-name-1.0.2"));
        assertThat(inflatedTag.version(), equalTo("0"));
        assertThat(inflatedTag.buildNumber(), equalTo(0L));
    }

    @Test
    public void tagIsFormattedAsExpectedWithProjectPrefixTemplate() {
        Log log = Mockito.mock(Log.class);
        String name = "release";
        String groupId = "my-group";
        String artifactId = "my-app";
        String version = "1.0.0";
        String tagNameFormat = "@{project.groupId}-@{project.artifactId}-@{project.version}";
        String expected = "my-group-my-app-1.0.0";
        String result1 = AnnotatedTag.formatTagName(name, groupId, artifactId, version, tagNameFormat, log);
        assertThat(result1, Matchers.equalTo(expected));
    }

    @Test
    public void tagIsFormattedAsExpectedWithEmptyTemplate() {
        Log log = Mockito.mock(Log.class);
        String name = "release";
        String groupId = "my-group";
        String artifactId = "my-app";
        String version = "1.0.0";
        String tagNameFormat = "";
        String expected = "";
        String result = AnnotatedTag.formatTagName(name, groupId, artifactId, version, tagNameFormat, log);
        assertThat(result, Matchers.equalTo(expected));
    }

    @Test
    public void tagIsFormattedAsExpectedWith0nlyVersionTemplate() {
        Log log = Mockito.mock(Log.class);
        String name = "release";
        String groupId = "my-group";
        String artifactId = "my-app";
        String version = "1.0.0";
        String tagNameFormat = "@{version}";
        String expected = "1.0.0";
        String result = AnnotatedTag.formatTagName(name, groupId, artifactId, version, tagNameFormat, log);
        assertThat(result, Matchers.equalTo(expected));
    }

    @Test
    public void testWithNullTemplate() {
        Log log = Mockito.mock(Log.class);
        String name = "release";
        String groupId = "my-group";
        String artifactId = "my-app";
        String version = "1.0.0";
        String tagNameFormat = null;
        String expected = "";
        String result = AnnotatedTag.formatTagName(name, groupId, artifactId, version, tagNameFormat, log);
        assertThat(result, Matchers.equalTo(expected));
    }

    @Test
    public void testWithInvalidTemplate() {
        Log log = Mockito.mock(Log.class);
        String name = " release ";
        String groupId = "my-group";
        String artifactId = "my-app";
        String version = "1.0.0";
        String tagNameFormat = "@{$";
        String expected = "@{$";
        String result = AnnotatedTag.formatTagName(name, groupId, artifactId, version, tagNameFormat, log);
        assertThat(result, Matchers.equalTo(expected));
    }
}
