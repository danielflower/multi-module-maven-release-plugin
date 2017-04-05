package e2e;

import scaffolding.MavenExecutionException;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.TestUtils;

public class TestRunningTest {

    @Rule
    public TestProject projectWithTestsThatFail = new TestProject(ProjectType.MODULE_WITH_TEST_FAILURE);
    private String     expectedTagName          = "";

    @Before
    public void setUp() {
        expectedTagName = TestUtils.tagNameStart();
    }

    @Test
    public void doesNotReleaseIfThereAreTestFailuresButTagsAreStillWritten() throws Exception {
        try {
            projectWithTestsThatFail.mvnRelease();
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {
        }
        assertThat(projectWithTestsThatFail.local, hasCleanWorkingDirectory());
        assertThat(projectWithTestsThatFail.local.tagList().call().get(0).getName(), startsWith(expectedTagName));
        assertThat(projectWithTestsThatFail.origin.tagList().call().get(0).getName(), startsWith(expectedTagName));
    }

    @Test
    public void ifTestsAreSkippedYouCanReleaseWithoutRunningThem() throws IOException {
        projectWithTestsThatFail.mvn("-DskipTests", "releaser:release");
    }
}
