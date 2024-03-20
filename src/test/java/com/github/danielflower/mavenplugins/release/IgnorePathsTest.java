package com.github.danielflower.mavenplugins.release;

import static com.github.danielflower.mavenplugins.release.AnnotatedTagFinderTest.saveFileInModuleAndTag;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Sets;

import scaffolding.TestProject;

@RunWith(Parameterized.class)
public class IgnorePathsTest {
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
        tag = saveFileInModuleAndTag(project, "server-modules", "1.0", 0);
        // create & commit three more files but do NOT create a tag at that revision
        // -> changes in the project (-> signal for the plugin to release those)
        project.commitFile("server-modules", "paul.txt");
        project.commitFile("core-utils", "muller.txt");
        project.commitFile("core-utils", "dave.txt");
        project.commitFile("core-utils", "john.txt");
        project.commitFile("parent-module", "muller.txt");
        project.commitFile("parent-module", "dave.txt");
        project.commitFile("parent-module", "john.txt");
        project.commitFile(".", "4711.txt");
    }

    @Parameters(name = "{0} -> has changes: {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new Object[]{"**.txt", false}, // all .txt are ignored -> no (other) changes
            new Object[]{"core-utils", true}, // only 1 directory ignored -> there are (other) changes
            new Object[]{"core-utils/muller.txt", true}, // only 1 file in 1 directory ignored -> there are (other) changes
            new Object[]{"4711.txt", true}, // only 1 file ignored -> there are (other) changes
            new Object[]{null, true}, // no file ignored -> there are (other) changes
            new Object[]{"server-modules/paul.txt,core-utils/,parent-module/,4711.txt", false}, // all file ignored -> no (other) changes
            new Object[]{"server-modules/paul.txt,core-utils/muller.txt,core-utils/dave.txt,core-utils/john.txt,parent-module/muller.txt,parent-module/dave.txt,parent-module/john.txt,4711.txt", false} // all file ignored -> no (other) changes
        );
    }

    @Test
    public void shouldConsiderIgnoredPathsWhenDetectingChanges() throws IOException {
        // given the test project in setup() @BeforeClass
        Set<String> ignoredPaths = Objects.isNull(ignoredPathsPattern) ? null : Sets.newHashSet(ignoredPathsPattern.split(",|;"));
        DiffDetector detector = new TreeWalkingDiffDetector(project.local.getRepository(), ignoredPaths);

        // when
        boolean hasChangedSince = detector.hasChangedSince(".", Collections.emptyList(), singletonListOf(tag));

        // then
        assertThat(hasChangedSince, is(changeExpected));
    }

    private static Collection<AnnotatedTag> singletonListOf(AnnotatedTag tag) {
        return Collections.singletonList(tag);
    }


}
