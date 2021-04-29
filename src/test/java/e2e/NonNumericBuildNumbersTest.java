package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;

import static scaffolding.TestProject.differentDelimiterProject;
import static scaffolding.TestProject.emptyBuildNumberInPomProject;
import static scaffolding.TestProject.singleModuleProject;

/**
 * Tests behavior when the build number is not a number. It can be alpha-numeric or an empty string in which case it
 * should be omitted in the release version.
 */
@RunWith(Parameterized.class)
public class NonNumericBuildNumbersTest {
    @Parameter
    public String testCaseName;

    @Parameter(1)
    public TestProject testProject;

    @Parameter(2)
    public String buildNumber;

    @Parameter(3)
    public String expectedReleaseVersion;

    @BeforeClass
    public static void installPluginToLocalRepo() {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Parameters(name = "{0} -> {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
            // The projects used here are arbitrary, you just can't use the same project + build number combination twice
            new Object[]{"alpha-numeric build number as CLI argument", differentDelimiterProject(), "ABC", "1.0.0-ABC"},
            new Object[]{"empty build number as CLI argument, ''", differentDelimiterProject(), "''", "1.0.0"},
            new Object[]{"empty build number as CLI argument, \"\"", singleModuleProject(), "\"\"", "1.0"},
            // This one is specific because of the <buildNumber> in the POM
            new Object[]{"empty build number in POM", emptyBuildNumberInPomProject(), null, "1.0.0"}
        );
    }

    @Test
    public void shouldSupportAlphaNumericOrEmptyBuildNumbers() throws Exception {
        // given the test data
        List<String> outputLines;

        // when
        if (buildNumber == null) {
            outputLines = testProject.mvnRelease();
        } else {
            outputLines = testProject.mvnRelease(buildNumber);
        }

        // then
        assertMainClassOutputWithReleaseVersion(outputLines);
        assertJarWithVersionInTargetFolder();
        assertArtifactInLocalMavenRepo();
    }

    private void assertMainClassOutputWithReleaseVersion(List<String> outputLines) {
        assertThat(outputLines, oneOf(containsString("Hello from version " + expectedReleaseVersion + "!")));
    }

    private void assertArtifactInLocalMavenRepo() throws IOException, MavenInvocationException {
        MvnRunner.assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects", testProject.getName(), expectedReleaseVersion);
    }

    private void assertJarWithVersionInTargetFolder() {
        String fileName = testProject.getName() + "-" + expectedReleaseVersion + "-package.jar";
        File jarInTargetFolder = new File(testProject.localDir, "target/" + fileName);
        assertThat(jarInTargetFolder.exists(), is(true));
    }
}
