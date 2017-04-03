package com.github.danielflower.mavenplugins.release.repository;

import scaffolding.TestProject;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
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
