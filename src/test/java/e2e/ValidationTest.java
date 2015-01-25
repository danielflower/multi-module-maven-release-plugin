package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;

public class ValidationTest {

    final TestProject testProject = TestProject.singleModuleProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void ifTheSameVersionIsReleasedTwiceItErrorsLoudly() throws Exception {
        testProject.mvnRelease("1");
        try {
            testProject.mvnRelease("1");
            Assert.fail("Should not have worked the second time");
        } catch (MavenExecutionException mee) {
            assertThat(mee.output,
                twoOf(containsString("There is already a tag named single-module-1.0.1 in this repository.")));
            assertThat(mee.output,
                oneOf(containsString("It is likely that this version has been released before.")));
            assertThat(mee.output,
                oneOf(containsString("Please try incrementing the release version and trying again.")));
        }
    }

}
