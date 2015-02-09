package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;
import scaffolding.TestProject;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LocalGitRepoTest {

    TestProject project = TestProject.singleModuleProject();

    @Test
    public void canDetectLocalTags() throws GitAPIException {
        LocalGitRepo repo = new LocalGitRepo(project.local);
        tag(project.local, "some-tag");
        assertThat(repo.hasLocalTag("some-tag"), is(true));
        assertThat(repo.hasLocalTag("some-ta"), is(false));
        assertThat(repo.hasLocalTag("some-tagyo"), is(false));
    }

    @Test
    public void canDetectRemoteTags() throws Exception {
        LocalGitRepo repo = new LocalGitRepo(project.local);
        tag(project.origin, "some-tag");
        assertThat(repo.remoteTagsFrom(asList("blah", "some-tag")), equalTo(asList("some-tag")));
        assertThat(repo.remoteTagsFrom(asList("blah", "some-taggart")), equalTo(emptyList()));
    }

    @Test
    public void canHaveHundredsOfTags() throws GitAPIException {
        for (int i = 0; i < 500; i++) {
            tag(project.local, "this-is-a-tag-" + i);
        }
        project.local.push().setPushTags().call();
        LocalGitRepo repo = new LocalGitRepo(project.local);
        for (int i = 0; i < 500; i++) {
            String tagName = "this-is-a-tag-" + i;
            assertThat(repo.hasLocalTag(tagName), is(true));
            assertThat(repo.remoteTagsFrom(asList(tagName)).size(), is(1));
        }
    }

    private static List<String> emptyList() {
        return new ArrayList<String>();
    }

    private static void tag(Git repo, String name) throws GitAPIException {
        repo.tag().setAnnotated(true).setName(name).setMessage("Some message").call();
    }
}
