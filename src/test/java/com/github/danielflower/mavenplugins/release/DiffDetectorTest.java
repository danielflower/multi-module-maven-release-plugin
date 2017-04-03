package com.github.danielflower.mavenplugins.release;

import scaffolding.TestProject;

import static com.github.danielflower.mavenplugins.release.TestUtils.saveFileInModule;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.releaseinfo.ReleaseInfoStorage;

public class DiffDetectorTest {

    @Test
    public void canDetectIfFilesHaveBeenChangedForAModuleSinceSomeSpecificTag() throws Exception {
        TestProject project = TestProject.independentVersionsProject();

        AnnotatedTag tag1 = saveFileInModule(project, "console-app", "1.2.3");
        AnnotatedTag tag2 = saveFileInModule(project, "core-utils", "2.0");
        AnnotatedTag tag3 = saveFileInModule(project, "console-app", "1.2.4");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository());

        assertThat(detector.hasChangedSince("core-utils", Collections.emptyList(), tag2.ref()), is(false));
        assertThat(detector.hasChangedSince("console-app", Collections.emptyList(), tag2.ref()), is(true));
        assertThat(detector.hasChangedSince("console-app", Collections.emptyList(), tag3.ref()), is(false));
    }

    @Test
    public void canDetectThingsInTheRoot() throws IOException, GitAPIException {
        TestProject simple = TestProject.singleModuleProject();
        AnnotatedTag tag1 = saveFileInModule(simple, ".", "1.0.1");
        simple.commitRandomFile(".");
        AnnotatedTag tag2 = saveFileInModule(simple, ".", "1.0.2");

        DiffDetector detector = new TreeWalkingDiffDetector(simple.local.getRepository());

        assertThat(detector.hasChangedSince(".", Collections.emptyList(), tag1.ref()), is(true));
        assertThat(detector.hasChangedSince(".", Collections.emptyList(), tag2.ref()), is(false));
    }

    @Test
    public void ignoreReleaseInfoInTheRoot() throws IOException, GitAPIException {
        TestProject simple = TestProject.singleModuleProject();
        AnnotatedTag tag1 = saveFileInModule(simple, ".", "1.0.1");
        simple.commitFile(".", ReleaseInfoStorage.RELEASE_INFO_FILE, "any-content");
        DiffDetector detector = new TreeWalkingDiffDetector(simple.local.getRepository());
        assertThat(detector.hasChangedSince(".", Collections.emptyList(), tag1.ref()), is(false));

        AnnotatedTag tag2 = saveFileInModule(simple, ".", "1.0.2");
        assertThat(detector.hasChangedSince(".", Collections.emptyList(), tag2.ref()), is(false));
    }

    @Test
    public void canDetectChangesAfterTheLastTag() throws IOException, GitAPIException {
        TestProject project = TestProject.independentVersionsProject();

        saveFileInModule(project, "console-app", "1.2.3");
        saveFileInModule(project, "core-utils", "2.0");
        AnnotatedTag tag3 = saveFileInModule(project, "console-app", "1.2.4");
        project.commitRandomFile("console-app");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository());
        assertThat(detector.hasChangedSince("console-app", Collections.emptyList(), tag3.ref()), is(true));
    }

    @Test
    public void canIgnoreModuleFolders() throws IOException, GitAPIException {
        TestProject project = TestProject.independentVersionsProject();

        saveFileInModule(project, "console-app", "1.2.3");
        saveFileInModule(project, "core-utils", "2.0");
        AnnotatedTag tag3 = saveFileInModule(project, "console-app", "1.2.4");
        project.commitRandomFile("console-app");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository());
        assertThat(detector.hasChangedSince("console-app", singletonList("console-app"), tag3.ref()), is(false));
    }
}
