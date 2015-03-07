package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;
import scaffolding.TestProject;

import java.io.IOException;

import static com.github.danielflower.mavenplugins.release.AnnotatedTagFinder.isPotentiallySameVersionIgnoringBuildNumber;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

public class AnnotatedTagFinderTest {
    @Test
    public void findsTheLatestCommitWhereThereHaveBeenNoBranches() throws Exception {
        TestProject project = TestProject.independentVersionsProject();

        saveFileInModule(project, "console-app", "1.2", 3);
        AnnotatedTag tag2 = saveFileInModule(project, "core-utils", "2", 0);
        AnnotatedTag tag3 = saveFileInModule(project, "console-app", "1.2", 4);

        assertThat(AnnotatedTagFinder.mostRecent(project.local, "console-app", "1.3"), hasSize(0));
        assertThat(AnnotatedTagFinder.mostRecent(project.local, "console-app", "1.2"), contains(tag3));
        assertThat(AnnotatedTagFinder.mostRecent(project.local, "core-utils", "2"), contains(tag2));
    }

    static AnnotatedTag saveFileInModule(TestProject project, String moduleName, String version, long buildNumber) throws IOException, GitAPIException {
        project.commitRandomFile(moduleName);
        String nameForTag = moduleName.equals(".") ? "root" : moduleName;
        AnnotatedTag tag = AnnotatedTag.create(nameForTag + "-" + version + "." + buildNumber, version, buildNumber);
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


}
