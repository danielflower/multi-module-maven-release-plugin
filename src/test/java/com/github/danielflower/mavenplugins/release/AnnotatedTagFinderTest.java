package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;
import scaffolding.TestProject;

import java.io.IOException;
import java.util.List;

import static com.github.danielflower.mavenplugins.release.AnnotatedTagFinder.isPotentiallySameVersionIgnoringBuildNumber;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AnnotatedTagFinderTest {
    @Test
    public void findsTheLatestCommitWhereThereHaveBeenNoBranches() throws Exception {
        TestProject project = TestProject.independentVersionsProject();

        AnnotatedTag tag1 = saveFileInModule(project, "console-app", "1.2", 3);
        AnnotatedTag tag2 = saveFileInModule(project, "core-utils", "2", 0);
        AnnotatedTag tag3 = saveFileInModule(project, "console-app", "1.2", 4);

        assertThat(AnnotatedTagFinder.tagsForVersion(project.local, "console-app", "1.3"), hasSize(0));
        assertThat(AnnotatedTagFinder.tagsForVersion(project.local, "console-app", "1.2"), containsInAnyOrder(tag1, tag3));
        assertThat(AnnotatedTagFinder.tagsForVersion(project.local, "core-utils", "2"), contains(tag2));
    }

    static AnnotatedTag saveFileInModule(TestProject project, String moduleName, String version, long buildNumber) throws IOException, GitAPIException {
        project.commitRandomFile(moduleName);
        String nameForTag = moduleName.equals(".") ? "root" : moduleName;
        return tagLocalRepo(project, nameForTag + "-" + version + "." + buildNumber, version, buildNumber);
    }

    private static AnnotatedTag tagLocalRepo(TestProject project, String tagName, String version, long buildNumber) throws GitAPIException {
        AnnotatedTag tag = AnnotatedTag.create(tagName, version, buildNumber);
        tag.saveAtHEAD(project.local);
        return tag;
    }

    @Test
    public void canRecogniseTagsThatArePotentiallyOfTheSameVersion() {
        assertThat(isPotentiallySameVersionIgnoringBuildNumber("my-artifact-1.2", "refs/tags/my-artifact-1.2.2"), is(true));
        assertThat(isPotentiallySameVersionIgnoringBuildNumber("my-artifact-1.2", "refs/tags/my-artifact-1.20.2"), is(false));
        assertThat(isPotentiallySameVersionIgnoringBuildNumber("my-artifact-1.3", "refs/tags/my-artifact-1.2.2"), is(false));
        assertThat(isPotentiallySameVersionIgnoringBuildNumber("not-my-artifact-1.2", "refs/tags/my-artifact-1.2.2"), is(false));
    }

    @Test
    public void returnsMultipleTagsOnASingleCommit() throws IOException, GitAPIException, MojoExecutionException, ValidationException {
        TestProject project = TestProject.independentVersionsProject();
        saveFileInModule(project, "console-app", "1.2", 1);
        AnnotatedTag tag1 = tagLocalRepo(project, "console-app-1.1.1.1", "1.1.1", 1);
        AnnotatedTag tag3 = tagLocalRepo(project, "console-app-1.1.1.3", "1.1.3", 3);
        AnnotatedTag tag2 = tagLocalRepo(project, "console-app-1.1.1.2", "1.1.2", 2);
        List<AnnotatedTag> annotatedTags = AnnotatedTagFinder.tagsForVersion(project.local, "console-app", "1.1");
        assertThat(annotatedTags, containsInAnyOrder(tag1, tag2, tag3));
        VersionNamer versionNamer = new VersionNamer();
        VersionName name = versionNamer.name("1.1.1", null, annotatedTags);
        assertThat(name.releaseVersion(), equalTo("1.1.1.4"));
    }


}
