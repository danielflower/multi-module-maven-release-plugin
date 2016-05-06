package com.github.danielflower.mavenplugins.release.scm;

import static com.github.danielflower.mavenplugins.release.scm.AnnotatedTagFinderTest.saveFileInModule;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;

import scaffolding.TestProject;

public class DiffDetectorTest {

	@Test
	public void canDetectIfFilesHaveBeenChangedForAModuleSinceSomeSpecificTag() throws Exception {
		final TestProject project = TestProject.independentVersionsProject();

		final ProposedTag tag1 = saveFileInModule(project, "console-app", "1.2", 3);
		final ProposedTag tag2 = saveFileInModule(project, "core-utils", "2", 0);
		final ProposedTag tag3 = saveFileInModule(project, "console-app", "1.2", 4);

		final DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository());

		assertThat(detector.hasChangedSince("core-utils", noChildModules(), asList(tag2)), is(false));
		assertThat(detector.hasChangedSince("console-app", noChildModules(), asList(tag2)), is(true));
		assertThat(detector.hasChangedSince("console-app", noChildModules(), asList(tag3)), is(false));
	}

	@Test
	public void canDetectThingsInTheRoot() throws IOException, GitAPIException {
		final TestProject simple = TestProject.singleModuleProject();
		final ProposedTag tag1 = saveFileInModule(simple, ".", "1.0", 1);
		simple.commitRandomFile(".");
		final DiffDetector detector = new TreeWalkingDiffDetector(simple.local.getRepository());
		assertThat(detector.hasChangedSince(".", noChildModules(), asList(tag1)), is(true));

		final ProposedTag tag2 = saveFileInModule(simple, ".", "1.0", 2);
		assertThat(detector.hasChangedSince(".", noChildModules(), asList(tag2)), is(false));
	}

	@Test
	public void canDetectChangesAfterTheLastTag() throws IOException, GitAPIException {
		final TestProject project = TestProject.independentVersionsProject();

		saveFileInModule(project, "console-app", "1.2", 3);
		saveFileInModule(project, "core-utils", "2", 0);
		final ProposedTag tag3 = saveFileInModule(project, "console-app", "1.2", 4);
		project.commitRandomFile("console-app");

		final DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository());
		assertThat(detector.hasChangedSince("console-app", noChildModules(), asList(tag3)), is(true));
	}

	@Test
	public void canIgnoreModuleFolders() throws IOException, GitAPIException {
		final TestProject project = TestProject.independentVersionsProject();

		saveFileInModule(project, "console-app", "1.2", 3);
		saveFileInModule(project, "core-utils", "2", 0);
		final ProposedTag tag3 = saveFileInModule(project, "console-app", "1.2", 4);
		project.commitRandomFile("console-app");

		final DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository());
		assertThat(detector.hasChangedSince("console-app", asList("console-app"), asList(tag3)), is(false));
	}

	private static java.util.List<String> noChildModules() {
		return new ArrayList<String>();
	}
}
