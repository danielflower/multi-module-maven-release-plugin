package de.hilling.maven.release;

import e2e.ProjectType;
import scaffolding.TestProject;

import static de.hilling.maven.release.TestUtils.saveFileInModule;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.releaseinfo.ReleaseInfoStorage;

public class DiffDetectorTest {

    @Rule
    public TestProject singleProject = new TestProject(ProjectType.SINGLE);
    @Rule
    public TestProject independentVersions = new TestProject(ProjectType.INDEPENDENT_VERSIONS);

    @Before
    public void setUp() {
        singleProject.checkClean = false;
        independentVersions.checkClean = false;
    }

    @Test
    public void canDetectIfFilesHaveBeenChangedForAModuleSinceSomeSpecificTag() throws Exception {

        AnnotatedTag tag1 = saveFileInModule(independentVersions, "console-app", "1.2.3");
        AnnotatedTag tag2 = saveFileInModule(independentVersions, "core-utils", "2.0");
        AnnotatedTag tag3 = saveFileInModule(independentVersions, "console-app", "1.2.4");

        TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(independentVersions.local.getRepository());

        assertThat(detector.hasChangedSince("core-utils", Collections.emptyList(), tag2.ref()), is(false));
        assertThat(detector.hasChangedSince("console-app", Collections.emptyList(), tag2.ref()), is(true));
        assertThat(detector.hasChangedSince("console-app", Collections.emptyList(), tag3.ref()), is(false));
    }

    @Test
    public void canDetectThingsInTheRoot() throws IOException, GitAPIException {
        AnnotatedTag tag1 = saveFileInModule(singleProject, ".", "1.0.1");
        singleProject.commitRandomFile(".");
        AnnotatedTag tag2 = saveFileInModule(singleProject, ".", "1.0.2");

        TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(singleProject.local.getRepository());

        assertThat(detector.hasChangedSince(".", Collections.emptyList(), tag1.ref()), is(true));
        assertThat(detector.hasChangedSince(".", Collections.emptyList(), tag2.ref()), is(false));
    }

    @Test
    public void ignoreReleaseInfoInTheRoot() throws IOException, GitAPIException {
        AnnotatedTag tag1 = saveFileInModule(singleProject, ".", "1.0.1");
        singleProject.commitFile(".", ReleaseInfoStorage.RELEASE_INFO_FILE, "any-content");
        TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(singleProject.local.getRepository());
        assertThat(detector.hasChangedSince(".", Collections.emptyList(), tag1.ref()), is(false));

        AnnotatedTag tag2 = saveFileInModule(singleProject, ".", "1.0.2");
        assertThat(detector.hasChangedSince(".", Collections.emptyList(), tag2.ref()), is(false));
    }

    @Test
    public void canDetectChangesAfterTheLastTag() throws IOException, GitAPIException {
        saveFileInModule(independentVersions, "console-app", "1.2.3");
        saveFileInModule(independentVersions, "core-utils", "2.0");
        AnnotatedTag tag3 = saveFileInModule(independentVersions, "console-app", "1.2.4");
        independentVersions.commitRandomFile("console-app");

        TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(independentVersions.local.getRepository());
        assertThat(detector.hasChangedSince("console-app", Collections.emptyList(), tag3.ref()), is(true));
    }

    @Test
    public void canIgnoreModuleFolders() throws IOException, GitAPIException {
        saveFileInModule(independentVersions, "console-app", "1.2.3");
        saveFileInModule(independentVersions, "core-utils", "2.0");
        AnnotatedTag tag3 = saveFileInModule(independentVersions, "console-app", "1.2.4");
        independentVersions.commitRandomFile("console-app");

        TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(independentVersions.local.getRepository());
        assertThat(detector.hasChangedSince(".", singletonList("console-app"), tag3.ref()), is(false));
    }
}
