package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;
import scaffolding.TestProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import static com.github.danielflower.mavenplugins.release.AnnotatedTagFinderTest.saveFileInModuleAndTag;
import static com.github.danielflower.mavenplugins.release.AnnotatedTagFinderTest.tagCurrentCommit;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DiffDetectorTest {

    @Test
    public void canDetectIfFilesHaveBeenChangedForAModuleSinceSomeSpecificTag() throws Exception {
        TestProject project = TestProject.independentVersionsProject();

        AnnotatedTag tag1 = saveFileInModuleAndTag(project, "console-app", "1.2", 3);
        AnnotatedTag tag2 = saveFileInModuleAndTag(project, "core-utils", "2", 0);
        AnnotatedTag tag3 = saveFileInModuleAndTag(project, "console-app", "1.2", 4);

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), null, null);

        assertThat(detector.hasChangedSince("core-utils", noChildModules(), asList(tag2)), is(false));
        assertThat(detector.hasChangedSince("console-app", noChildModules(), asList(tag2)), is(true));
        assertThat(detector.hasChangedSince("console-app", noChildModules(), asList(tag3)), is(false));
    }

    @Test
    public void canDetectThingsInTheRoot() throws IOException, GitAPIException {
        TestProject simple = TestProject.singleModuleProject();
        AnnotatedTag tag1 = saveFileInModuleAndTag(simple, ".", "1.0", 1);
        simple.commitRandomFile(".");
        DiffDetector detector = new TreeWalkingDiffDetector(simple.local.getRepository(), null, null);
        assertThat(detector.hasChangedSince(".", noChildModules(), asList(tag1)), is(true));

        AnnotatedTag tag2 = saveFileInModuleAndTag(simple, ".", "1.0", 2);
        assertThat(detector.hasChangedSince(".", noChildModules(), asList(tag1, tag2)), is(false));
        assertThat(detector.hasChangedSince(".", noChildModules(), asList(tag2, tag1)), is(false));
    }

    @Test
    public void canDetectChangesAfterTheLastTag() throws IOException, GitAPIException {
        TestProject project = TestProject.independentVersionsProject();

        saveFileInModuleAndTag(project, "console-app", "1.2", 3);
        saveFileInModuleAndTag(project, "core-utils", "2", 0);
        AnnotatedTag tag3 = saveFileInModuleAndTag(project, "console-app", "1.2", 4);
        project.commitRandomFile("console-app");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), null, null);
        assertThat(detector.hasChangedSince("console-app", noChildModules(), asList(tag3)), is(true));
    }

    @Test
    public void canIgnoreChangesInModuleFolders() throws IOException, GitAPIException {
        TestProject project = TestProject.nestedProject();

        AnnotatedTag tag1 = saveFileInModuleAndTag(project, "server-modules", "1.0.2.4", 0);
        project.commitRandomFile("server-modules/server-module-a");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), null, null);
        assertThat(detector.hasChangedSince("server-modules", asList("server-module-a", "server-module-b"),
            asList(tag1)), is(false));
    }

    @Test
    public void canDetectLocalChangesWithModuleFolders() throws IOException, GitAPIException {
        TestProject project = TestProject.nestedProject();

        AnnotatedTag tag1 = saveFileInModuleAndTag(project, "server-modules", "1.0.2.4", 0);
        project.commitRandomFile("server-modules");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), null, null);
        assertThat(detector.hasChangedSince("server-modules", asList("server-module-a", "server-module-b"),
            asList(tag1)), is(true));
    }

    @Test
    public void canDetectLatestTagOnBranch() throws Exception {
        TestProject project = TestProject.singleModuleProject();
        AnnotatedTag tag1 = saveFileInModuleAndTag(project, ".", "1.0", 0);
        project.createBranch("feature");
        project.commitRandomFile(".");
        AnnotatedTag tag2 = saveFileInModuleAndTag(project, ".", "1.0", 1);
        project.checkoutBranch("feature");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), null, null);
        assertThat(detector.hasChangedSince(".", noChildModules(),
            asList(tag1, tag2)), is(false));
    }

    @Test
    public void canDetectLatestBuild() throws Exception {
        TestProject project = TestProject.singleModuleProject();
        AnnotatedTag tag1 = saveFileInModuleAndTag(project, ".", "1.0", 0);
        AnnotatedTag tag2 = tagCurrentCommit(project, ".", "1.0", 1);

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), null, null);
        assertThat(detector.hasChangedSince(".", noChildModules(),
            asList(tag1, tag2)), is(false));
        assertThat(detector.hasChangedSince(".", noChildModules(),
            asList(tag2, tag1)), is(false));
    }

    @Test
    public void canDetectLatestBuildNoMatterIfCreatedWrongWayRound() throws Exception {
        TestProject project = TestProject.singleModuleProject();
        AnnotatedTag tag2 = saveFileInModuleAndTag(project, ".", "1.0", 1);
        AnnotatedTag tag1 = tagCurrentCommit(project, ".", "1.0", 0);

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), null, null);
        assertThat(detector.hasChangedSince(".", noChildModules(),
            asList(tag2, tag1)), is(false));
        assertThat(detector.hasChangedSince(".", noChildModules(),
            asList(tag1, tag2)), is(false));
    }

    @Test
    public void canFilterOutChangesInExcludedPath() throws Exception {
        TestProject project = TestProject.singleModuleProject();
        String ignoredFile = "file.txt";
        AnnotatedTag tag1 = tagCurrentCommit(project, ".", "1.0", 0);
        project.commitFile(".", ignoredFile);

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), Collections.singletonMap(ignoredFile, null).keySet(), null);
        assertThat(detector.hasChangedSince(".", noChildModules(), Collections.singletonList(tag1)), is(false));
        detector = new TreeWalkingDiffDetector(project.local.getRepository(), Collections.singletonMap("/" + ignoredFile, null).keySet(), null);
        assertThat(detector.hasChangedSince(".", noChildModules(), Collections.singletonList(tag1)), is(false));
    }

    @Test
    public void canDetectChangesWithFilteredOutExcludedPaths() throws Exception {
        TestProject project = TestProject.singleModuleProject();
        String ignoredFile = "file.txt";
        AnnotatedTag tag1 = tagCurrentCommit(project, ".", "1.0", 0);
        project.commitFile(".", ignoredFile);
        project.commitRandomFile(".");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), Collections.singletonMap(ignoredFile, null).keySet(), null);
        assertThat(detector.hasChangedSince(".", noChildModules(), Collections.singletonList(tag1)), is(true));
        detector = new TreeWalkingDiffDetector(project.local.getRepository(), Collections.singletonMap("/" + ignoredFile, null).keySet(), null);
        assertThat(detector.hasChangedSince(".", noChildModules(), Collections.singletonList(tag1)), is(true));
    }

    @Test
    public void canFilterOutChangesInExcludedPathWildcard() throws Exception {
        TestProject project = TestProject.singleModuleProject();
        String ignoredFile = "file.txt";
        AnnotatedTag tag1 = tagCurrentCommit(project, ".", "1.0", 0);
        for (int i = 0; i < 5; i++) {
            project.commitFile(".", i + ignoredFile);
        }

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), Collections.singletonMap(ignoredFile, null).keySet(), null);
        assertThat(detector.hasChangedSince(".", noChildModules(), Collections.singletonList(tag1)), is(false));
    }

    @Test
    public void canFilterOutExcludedChangesInSubmodule() throws Exception {
        TestProject project = TestProject.nestedProject();
        String ignoredFile = "file.txt";

        AnnotatedTag tag1 = tagCurrentCommit(project, ".", "1.0", 4);
        AnnotatedTag tag2 = tagCurrentCommit(project, "server-modules", "1.0", 4);
        project.commitFile("server-modules", ignoredFile);

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), Collections.singletonMap(ignoredFile, null).keySet(), null);
        assertThat(detector.hasChangedSince(".", Collections.singletonList("server-modules"), asList(tag1, tag2)), is(false));
        assertThat(detector.hasChangedSince("server-modules", asList("server-module-a", "server-module-b"), asList(tag1, tag2)), is(false));
    }

    @Test
    public void canIgnoreNotRequiredChanges() throws Exception {
        TestProject project = TestProject.singleModuleProject();
        String requiredFile = "file.txt";
        AnnotatedTag tag1 = tagCurrentCommit(project, ".", "1.0", 0);
        project.commitRandomFile(".");
        project.commitRandomFile(".");
        project.commitRandomFile(".");

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), null, Collections.singletonMap(requiredFile, null).keySet());
        assertThat(detector.hasChangedSince(".", noChildModules(), Collections.singletonList(tag1)), is(false));
        detector = new TreeWalkingDiffDetector(project.local.getRepository(), null, Collections.singletonMap("/" + requiredFile, null).keySet());
        assertThat(detector.hasChangedSince(".", noChildModules(), Collections.singletonList(tag1)), is(false));
    }

    @Test
    public void canSkipIgnoredPathSettingsIfRequiredPathsIsSpecified() throws Exception {
        TestProject project = TestProject.singleModuleProject();
        String requiredFile = "file.txt";
        Set<String> requiredPaths = Collections.singletonMap(requiredFile, null).keySet();
        AnnotatedTag tag1 = tagCurrentCommit(project, ".", "1.0", 0);
        project.commitFile(".", requiredFile);

        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), requiredPaths, requiredPaths);
        assertThat(detector.hasChangedSince(".", noChildModules(), Collections.singletonList(tag1)), is(true));
    }

    private static java.util.List<String> noChildModules() {
        return new ArrayList<String>();
    }
}
