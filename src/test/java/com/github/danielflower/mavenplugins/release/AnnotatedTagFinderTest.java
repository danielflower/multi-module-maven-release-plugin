package com.github.danielflower.mavenplugins.release;

import scaffolding.TestProject;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Test;

public class AnnotatedTagFinderTest {

    private AnnotatedTagFinder tagFinder;
    private AnnotatedTagFinder bugfixTagFinder;

    @Before
    public void setUp() {
        tagFinder = new AnnotatedTagFinder(false);
        bugfixTagFinder = new AnnotatedTagFinder(true);
    }

    @Test
    public void parseCorrectly() throws Exception {
        TestProject project = TestProject.independentVersionsProject();

        AnnotatedTag tag1 = saveFileInModule(project, "console-app", "1.23", 3, 23L);

        final List<AnnotatedTag> loadedTag = bugfixTagFinder.tagsForVersion(project.local, "console-app", "1");
        assertThat(loadedTag, hasSize(1));
        assertThat(loadedTag, contains(tag1));
        assertThat(loadedTag.get(0).versionInfo().getBugfixBranchNumber(), is(23L));
    }



    @Test
    public void findsTheLatestCommitWhereThereHaveBeenNoBranches() throws Exception {
        TestProject project = TestProject.independentVersionsProject();

        AnnotatedTag tag1 = saveFileInModule(project, "console-app", "1.2", 3, null);
        AnnotatedTag tag2 = saveFileInModule(project, "core-utils", "2", 0, null);
        AnnotatedTag tag3 = saveFileInModule(project, "console-app", "1.2", 4, null);

        assertThat(tagFinder.tagsForVersion(project.local, "console-app", "1.3"), hasSize(0));
        assertThat(tagFinder.tagsForVersion(project.local, "console-app", "1.2"), containsInAnyOrder(tag1, tag3));
        assertThat(tagFinder.tagsForVersion(project.local, "core-utils", "2"), contains(tag2));
    }

    @Test
    public void findBuildAndReleaseNumbers() throws Exception {
        TestProject project = TestProject.independentVersionsProject();
        AnnotatedTag tag1 = saveFileInModule(project, "console-app", "1", 2, null);
        AnnotatedTag tag2 = saveFileInModule(project, "console-app", "1.2", 1, null);
        final List<AnnotatedTag> annotatedTags = tagFinder.tagsForVersion(project.local, "console-app", "1");
        assertThat(annotatedTags, hasSize(1));

    }

    @Test
    public void findBuildAndReleaseNumbersForBugfixRelease() throws Exception {
        TestProject project = TestProject.independentVersionsProject();
        AnnotatedTag tag1 = saveFileInModule(project, "console-app", "1", 2, null);
        AnnotatedTag tag2 = saveFileInModule(project, "console-app", "1.2", 1, null);
        final List<AnnotatedTag> annotatedTags = bugfixTagFinder.tagsForVersion(project.local, "console-app", "1");
        assertThat(annotatedTags, hasSize(2));

    }

    static AnnotatedTag saveFileInModule(TestProject project, String moduleName, String version, long buildNumber,
                                         Long bugfixBranchNumber) throws IOException, GitAPIException {
        project.commitRandomFile(moduleName);
        String nameForTag = moduleName.equals(".") ? "root" : moduleName;
        return tagLocalRepo(project, nameForTag + "-" + version + "." + buildNumber, version, buildNumber,
                            bugfixBranchNumber);
    }

    private static AnnotatedTag tagLocalRepo(TestProject project, String tagName, String version, long buildNumber,
                                             Long bugfixBranchNumber) throws GitAPIException {
        AnnotatedTag tag = AnnotatedTag.create(tagName, version, new VersionInfo(bugfixBranchNumber, buildNumber));
        tag.saveAtHEAD(project.local);
        return tag;
    }

    @Test
    public void canRecogniseTagsThatArePotentiallyOfTheSameVersion() {
        assertThat(tagFinder.isPotentiallySameVersionIgnoringBuildNumber("my-artifact-1.2",
                                                                         "refs/tags/my-artifact-1.2.2"), is(true));
        assertThat(tagFinder.isPotentiallySameVersionIgnoringBuildNumber("my-artifact-1.2", "refs/tags/my-artifact-1.20.2"), is(false));
        assertThat(tagFinder.isPotentiallySameVersionIgnoringBuildNumber("my-artifact-1.3", "refs/tags/my-artifact-1.2.2"), is(false));
        assertThat(tagFinder.isPotentiallySameVersionIgnoringBuildNumber("not-my-artifact-1.2", "refs/tags/my-artifact-1.2.2"), is(false));
    }

    @Test
    public void returnsMultipleTagsOnASingleCommit() throws IOException, GitAPIException, MojoExecutionException, ValidationException {
        TestProject project = TestProject.independentVersionsProject();
        saveFileInModule(project, "console-app", "1.2", 1, null);
        AnnotatedTag tag1 = tagLocalRepo(project, "console-app-1.1.1.1", "1.1.1", 1, null);
        AnnotatedTag tag3 = tagLocalRepo(project, "console-app-1.1.1.3", "1.1.1", 3, null);
        AnnotatedTag tag2 = tagLocalRepo(project, "console-app-1.1.1.2", "1.1.1", 2, null);
        List<AnnotatedTag> annotatedTags = tagFinder.tagsForVersion(project.local, "console-app", "1.1.1");
        assertThat(annotatedTags, containsInAnyOrder(tag1, tag2, tag3));
    }

    @Test
    public void versionNamerCaresNotForOrderOfTags() throws ValidationException {
        VersionNamer versionNamer = new VersionNamer(false);
        VersionName name = versionNamer.name("1.1.1", null,
                                             asList(new VersionInfo(null, 1L), new VersionInfo(null, 2L), new VersionInfo(null,
                                                                                                                          3L)));
        assertThat(name.releaseVersion(), equalTo("1.1.1.4"));
    }


}
