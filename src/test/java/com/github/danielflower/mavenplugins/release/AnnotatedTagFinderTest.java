package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;
import scaffolding.TestProject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AnnotatedTagFinderTest {
    private static final VersionNamer versionNamer = new VersionNamer();
    private final AnnotatedTagFinder annotatedTagFinder = new AnnotatedTagFinder(versionNamer);

    @Test
    public void findsTheLatestCommitWhereThereHaveBeenNoBranches() throws Exception {
        TestProject project = TestProject.independentVersionsProject();

        AnnotatedTag tag1 = saveFileInModule(project, "console-app", "1.2", 3);
        AnnotatedTag tag2 = saveFileInModule(project, "core-utils", "2", 0);
        AnnotatedTag tag3 = saveFileInModule(project, "console-app", "1.2", 4);

        assertThat(annotatedTagFinder.tagsForVersion(project.local, "console-app", "1.3", "-"), hasSize(0));
        assertThat(annotatedTagFinder.tagsForVersion(project.local, "console-app", "1.2", "-"), containsInAnyOrder(tag1, tag3));
        assertThat(annotatedTagFinder.tagsForVersion(project.local, "core-utils", "2", "-"), contains(tag2));
    }

    static AnnotatedTag saveFileInModule(TestProject project, String moduleName, String version, long buildNumber) throws IOException, GitAPIException {
        project.commitRandomFile(moduleName);
        String nameForTag = moduleName.equals(".") ? "root" : moduleName;
        return tagLocalRepo(project, nameForTag + "-" + version + versionNamer.getDelimiter() + buildNumber, version, buildNumber);
    }

    private static AnnotatedTag tagLocalRepo(TestProject project, String tagName, String version, long buildNumber) throws GitAPIException {
        AnnotatedTag tag = AnnotatedTag.create(tagName, version, buildNumber);
        tag.saveAtHEAD(project.local);
        return tag;
    }

    @Test
    public void canRecogniseTagsThatArePotentiallyOfTheSameVersion() {
        assertThat(annotatedTagFinder.isPotentiallySameVersionIgnoringBuildNumber("my-artifact-1.2", "refs/tags/my-artifact-1.2.2"), is(true));
        assertThat(annotatedTagFinder.isPotentiallySameVersionIgnoringBuildNumber("my-artifact-1.2", "refs/tags/my-artifact-1.20.2"), is(false));
        assertThat(annotatedTagFinder.isPotentiallySameVersionIgnoringBuildNumber("my-artifact-1.3", "refs/tags/my-artifact-1.2.2"), is(false));
        assertThat(annotatedTagFinder.isPotentiallySameVersionIgnoringBuildNumber("not-my-artifact-1.2", "refs/tags/my-artifact-1.2.2"), is(false));
    }

    @Test
    public void returnsMultipleTagsOnASingleCommit() throws IOException, GitAPIException, MojoExecutionException {
        TestProject project = TestProject.independentVersionsProject();
        saveFileInModule(project, "console-app", "1.2", 1);
        AnnotatedTag tag1 = tagLocalRepo(project, "console-app-1.1.1.1", "1.1.1", 1);
        AnnotatedTag tag3 = tagLocalRepo(project, "console-app-1.1.1.3", "1.1.1", 3);
        AnnotatedTag tag2 = tagLocalRepo(project, "console-app-1.1.1.2", "1.1.1", 2);
        List<AnnotatedTag> annotatedTags = annotatedTagFinder.tagsForVersion(project.local, "console-app", "1.1.1", "-");
        assertThat(annotatedTags, containsInAnyOrder(tag1, tag2, tag3));
    }

    @Test
    public void versionNamerCaresNotForOrderOfTags() throws ValidationException {
        VersionNamer versionNamer = new VersionNamer();
        VersionName name = versionNamer.name("1.1.1", null, asList(1L, 3L, 2L));
        assertThat(name.releaseVersion(), equalTo("1.1.1.4"));
    }

    @Test
    public void respectDelimiterWhenExtractingBuildNumber() {
        String refPrefix = "refs/tags/";
        AnnotatedTagFinder tagFinder = new AnnotatedTagFinder(new VersionNamer("-"));
        // This should actually be implemented with a parameterized JUnit test but since this class isn't set up for
        // that yet we'll use our own data-driven harness.
        Arrays.asList(new Object[][]{
            new Object[]{"libs-1.0.0", refPrefix + "foo-1.0.0-1", null},    // artifact ID does not match -> expect null
            new Object[]{"libs-1.0.0", refPrefix + "libs-1.0.0-1", 1L},     // proper match with expected delimiter
            new Object[]{"libs-1.0.0", refPrefix + "libs-1.0.0.1", null},   // no delimiter match
            new Object[]{"libs-1.0.0", refPrefix + "libs-1.0.0@1", null},   // no delimiter match
            new Object[]{"libs-1.0.0", refPrefix + "libs-1.0.0-A", null}    // format exception on build number
        })
        .forEach(dataSet -> {
            final Object expectedBuildNumber = dataSet[2];
            final Long buildNumber = tagFinder.buildNumberOf(dataSet[0].toString(), dataSet[1].toString());
            assertThat(buildNumber, is(expectedBuildNumber));
        });
    }
}
