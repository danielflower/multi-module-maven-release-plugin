package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import scaffolding.TestProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.github.danielflower.mavenplugins.release.AnnotatedTagFinderTest.saveFileInModule;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Enclosed.class)
public class TreeWalkingDiffDetectorTest {

    @Test
    public void canDetectIfFilesHaveBeenChangedForAModuleSinceSomeSpecificTag() throws Exception {
        TestProject project = TestProject.independentVersionsProject();

        AnnotatedTag tag1 = saveFileInModule(project, "console-app", "1.2", "3");
        AnnotatedTag tag2 = saveFileInModule(project, "core-utils", "2", "0");
        AnnotatedTag tag3 = saveFileInModule(project, "console-app", "1.2", "4");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository());

        assertThat(detector.hasChangedSince("core-utils", noChildModules(), singletonListOf(tag1)), is(true));
        assertThat(detector.hasChangedSince("core-utils", noChildModules(), singletonListOf(tag2)), is(false));
        assertThat(detector.hasChangedSince("console-app", noChildModules(), singletonListOf(tag2)), is(true));
        assertThat(detector.hasChangedSince("console-app", noChildModules(), singletonListOf(tag3)), is(false));
    }

    @Test
    public void canDetectThingsInTheRoot() throws IOException, GitAPIException {
        TestProject simple = TestProject.singleModuleProject();
        AnnotatedTag tag1 = saveFileInModule(simple, ".", "1.0", "1");
        simple.commitRandomFile(".");
        DiffDetector detector = new TreeWalkingDiffDetector(simple.local.getRepository());
        assertThat(detector.hasChangedSince(".", noChildModules(), singletonListOf(tag1)), is(true));

        AnnotatedTag tag2 = saveFileInModule(simple, ".", "1.0", "2");
        assertThat(detector.hasChangedSince(".", noChildModules(), singletonListOf(tag2)), is(false));
    }

    @Test
    public void canDetectChangesAfterTheLastTag() throws IOException, GitAPIException {
        TestProject project = TestProject.independentVersionsProject();

        saveFileInModule(project, "console-app", "1.2", "3");
        saveFileInModule(project, "core-utils", "2", "0");
        AnnotatedTag tag3 = saveFileInModule(project, "console-app", "1.2", "4");
        project.commitRandomFile("console-app");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository());
        assertThat(detector.hasChangedSince("console-app", noChildModules(), singletonListOf(tag3)), is(true));
    }

    @Test
    public void canIgnoreChangesInModuleFolders() throws IOException, GitAPIException {
        TestProject project = TestProject.nestedProject();

        AnnotatedTag tag1 = saveFileInModule(project, "server-modules", "1.0.2.4", "0");
        project.commitRandomFile("server-modules/server-module-a");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository());
        assertThat(detector.hasChangedSince("server-modules", asList("server-module-a", "server-module-b"),
            singletonListOf(tag1)), is(false));
    }

    @Test
    public void canDetectLocalChangesWithModuleFolders() throws IOException, GitAPIException {
        TestProject project = TestProject.nestedProject();

        AnnotatedTag tag1 = saveFileInModule(project, "server-modules", "1.0.2.4", "0");
        project.commitRandomFile("server-modules");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository());
        assertThat(detector.hasChangedSince("server-modules", asList("server-module-a", "server-module-b"),
            singletonListOf(tag1)), is(true));
    }

    @RunWith(Parameterized.class)
    public static class IgnorePaths {
        private static TestProject project;
        private static AnnotatedTag tag;

        @Parameter
        public String ignoredPathsPattern;

        @Parameter(1)
        public boolean changeExpected;

        @BeforeClass
        public static void setup() throws IOException, GitAPIException {
            project = TestProject.nestedProject();
            // create & commit a random file, then create a tag at that revision
            tag = saveFileInModule(project, "server-modules", "1.0", "0");
            // create & commit three more files but do NOT create a tag at that revision
            // -> changes in the project (-> signal for the plugin to release those)
            project.commitFile("server-modules", "paul.txt");
            project.commitFile("core-utils", "muller.txt");
            project.commitFile(".", "4711.txt");
        }

        @Parameters(name = "{0} -> has changes: {1}")
        public static Collection<Object[]> data() {
            return Arrays.asList(
                new Object[]{"**.txt", false}, // all .txt are ignored -> no (other) changes
                new Object[]{"core-utils", true}, // only 1 directory ignored -> there are (other) changes
                new Object[]{"core-utils/muller.txt", true}, // only 1 file in 1 directory ignored -> there are (other) changes
                new Object[]{"4711.txt", true} // only 1 file ignored -> there are (other) changes
            );
        }

        @Test
        public void shouldConsiderIgnoredPathsWhenDetectingChanges() throws IOException {
            // given the test project in setup() @BeforeClass
            Set<String> ignoredPaths = Collections.singleton(ignoredPathsPattern);
            DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), ignoredPaths);

            // when
            boolean hasChangedSince = detector.hasChangedSince(".", Collections.emptyList(), singletonListOf(tag));

            // then
            assertThat(hasChangedSince, is(changeExpected));
        }
    }

    private static Collection<AnnotatedTag> singletonListOf(AnnotatedTag tag) {
        return Collections.singletonList(tag);
    }

    private static List<String> noChildModules() {
        return new ArrayList<>();
    }
}
