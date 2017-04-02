package com.github.danielflower.mavenplugins.release.repository;

import scaffolding.TestProject;

import static com.github.danielflower.mavenplugins.release.GitHelper.scmUrlToRemote;
import static java.util.Arrays.asList;
import static scaffolding.TestProject.dirToGitScmReference;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.AnnotatedTag;
import com.github.danielflower.mavenplugins.release.TestUtils;

public class LocalGitRepoTest {

    TestProject project = TestProject.singleModuleProject();


    @Test
    public void canDetectLocalTags() throws GitAPIException {
        LocalGitRepo repo = new LocalGitRepo(project.local, null);
        tag(project.local, "some-tag");
        MatcherAssert.assertThat(repo.hasLocalTag("some-tag"), CoreMatchers.is(true));
        MatcherAssert.assertThat(repo.hasLocalTag("some-ta"), CoreMatchers.is(false));
        MatcherAssert.assertThat(repo.hasLocalTag("some-tagyo"), CoreMatchers.is(false));
    }

    @Test
    public void canDetectRemoteTags() throws Exception {
        LocalGitRepo repo = new LocalGitRepo(project.local, null);
        tag(project.origin, "some-tag");
        MatcherAssert.assertThat(repo.remoteTagsFrom(tags("blah", "some-tag")), CoreMatchers.equalTo(asList("some-tag")));
        MatcherAssert.assertThat(repo.remoteTagsFrom(tags("blah", "some-taggart")), CoreMatchers.equalTo(emptyList()));
    }

    @Test
    public void usesThePassedInScmUrlToFindRemote() throws Exception {
        LocalGitRepo repo = new LocalGitRepo(project.local, scmUrlToRemote(dirToGitScmReference(project.originDir)));
        tag(project.origin, "some-tag");

        StoredConfig config = project.local.getRepository().getConfig();
        config.unsetSection("remote", "origin");
        config.save();

        MatcherAssert.assertThat(repo.remoteTagsFrom(tags("blah", "some-tag")), CoreMatchers.equalTo(asList("some-tag")));
    }

    @Test
    public void canHaveManyTags() throws GitAPIException {
        int numberOfTags = 50; // setting this to 1000 works but takes too long
        for (int i = 0; i < numberOfTags; i++) {
            tag(project.local, "this-is-a-tag-" + i);
        }
        project.local.push().setPushTags().call();
        LocalGitRepo repo = new LocalGitRepo(project.local, null);
        for (int i = 0; i < numberOfTags; i++) {
            String tagName = "this-is-a-tag-" + i;
            MatcherAssert.assertThat(repo.hasLocalTag(tagName), CoreMatchers.is(true));
            MatcherAssert.assertThat(repo.remoteTagsFrom(tags(tagName)).size(), CoreMatchers.is(1));
        }
    }

    private static List<AnnotatedTag> tags(String... tagNames) {
        List<AnnotatedTag> tags = new ArrayList<>();
        for (String tagName : tagNames) {
            tags.add(new AnnotatedTag(null, tagName, TestUtils.releaseInfo(2, 4, tagName, tagName)));
        }
        return tags;
    }
    private static List<String> emptyList() {
        return new ArrayList<>();
    }

    private static void tag(Git repo, String name) throws GitAPIException {
        repo.tag().setAnnotated(true).setName(name).setMessage("Some message").call();
    }
}
