package de.hilling.maven.release.repository;

import e2e.ProjectType;
import scaffolding.TestProject;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;

public class LocalGitRepoTest {

    @Rule
    public TestProject project = new TestProject(ProjectType.SINGLE);


    @Test
    public void canDetectLocalTags() throws GitAPIException {
        LocalGitRepo repo = new LocalGitRepo(project.local, null, null);
        tag(project.local, "some-tag");
        MatcherAssert.assertThat(repo.hasLocalTag("some-tag"), CoreMatchers.is(true));
        MatcherAssert.assertThat(repo.hasLocalTag("some-ta"), CoreMatchers.is(false));
        MatcherAssert.assertThat(repo.hasLocalTag("some-tagyo"), CoreMatchers.is(false));
    }

    private static void tag(Git repo, String name) throws GitAPIException {
        repo.tag().setAnnotated(true).setName(name).setMessage("Some message").call();
    }
}
