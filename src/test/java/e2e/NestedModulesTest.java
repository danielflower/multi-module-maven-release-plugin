package e2e;

import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTagWithModuleVersion;
import static scaffolding.MvnRunner.assertArtifactInLocalRepo;

import java.io.IOException;
import java.util.List;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.versioning.VersionMatcher;

public class NestedModulesTest {

    public static final String      GROUP_ID                     = "com.github.danielflower.mavenplugins.testprojects.nested";
    final               TestProject testProject                  = TestProject.nestedProject();
    private final       String      expectedAggregatorVersion    = "0.0";
    private final       String      expectedParentVersion        = "1.0";
    private final       String      expectedCoreVersion          = "2.0";
    private final       String      expectedAppVersion           = "3.0";
    private final       String      expectedServerModulesVersion = "1.0";
    private final       String      expectedServerModuleAVersion = "3.0";
    private final       String      expectedServerModuleBVersion = "3.0";
    private final       String      expectedServerModuleCVersion = "3.0";

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void buildsAndInstallsAndTagsAllModules() throws Exception {
        buildsEachProjectOnceAndOnlyOnce(testProject.mvnRelease());
        installsAllModulesIntoTheRepoWithTheBuildNumber();

        assertBothReposTagged("nested-project", expectedAggregatorVersion, "");
        assertBothReposTagged("core-utils", expectedCoreVersion, "");
        assertBothReposTagged("console-app", expectedAppVersion, "");
        assertBothReposTagged("parent-module", expectedParentVersion, "");
        assertBothReposTagged("server-modules", expectedServerModulesVersion, "");
        assertBothReposTagged("server-module-a", expectedServerModuleAVersion, "");
        assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "");
        assertBothReposTagged("server-module-c", expectedServerModuleCVersion, ".misnamed");

        testProject.commitRandomFile("server-modules/server-module-b");
        testProject.mvn("releaser:release");

        assertBothReposNotTagged("nested-project", minor(expectedAggregatorVersion, 1));
        assertBothReposNotTagged("core-utils", minor(expectedCoreVersion, 1));
        assertBothReposTagged("console-app", minor(expectedAppVersion, 1), "");
        assertBothReposNotTagged("parent-module", minor(expectedParentVersion, 1));
        assertBothReposNotTagged("server-modules", minor(expectedServerModulesVersion, 1));
        assertBothReposNotTagged("server-module-a", minor(expectedServerModuleAVersion,1 ));
        assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "");
        assertBothReposTagged("server-module-c", expectedServerModuleCVersion, ".misnamed");

        testProject.commitRandomFile("parent-module");
        testProject.mvn("releaser:release");

        assertBothReposNotTagged("nested-project", minor(expectedAggregatorVersion, 1));
        assertBothReposTagged("core-utils", minor(expectedCoreVersion, 1), "");
        assertBothReposTagged("console-app", minor(expectedAppVersion, 2), "");
        assertBothReposTagged("parent-module", minor(expectedParentVersion, 2), "");
        assertBothReposNotTagged("server-modules", minor(expectedServerModulesVersion, 2));
        assertBothReposTagged("server-module-a", minor(expectedServerModuleAVersion,2 ), "");
        assertBothReposTagged("server-module-b", minor(expectedServerModuleBVersion, 2), "");
        assertBothReposTagged("server-module-c", minor(expectedServerModuleCVersion, 2),  ".misnamed");

        testProject.mvnRelease();
        assertBothReposTagged("nested-project", expectedAggregatorVersion, "");
        assertBothReposTagged("core-utils", expectedCoreVersion, "");
        assertBothReposTagged("console-app", expectedAppVersion, "");
        assertBothReposTagged("parent-module", expectedParentVersion, "");
        assertBothReposTagged("server-modules", expectedServerModulesVersion, "");
        assertBothReposTagged("server-module-a", expectedServerModuleAVersion, "");
        assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "");
        assertBothReposTagged("server-module-c", expectedServerModuleCVersion, ".misnamed");
    }

    private String minor(String expectedCoreVersion, int newMinor) {
        return new VersionMatcher(expectedCoreVersion).fixVersion().withMinorVersion(newMinor).toString();
    }

    private void buildsEachProjectOnceAndOnlyOnce(List<String> commandOutput) throws Exception {
        assertThat(commandOutput, allOf(twoOf(containsString("Building nested-project")),
                                        // once for initial build; once for release build
                                        oneOf(containsString("Building core-utils")),
                                        oneOf(containsString("Building console-app")),
                                        oneOf(containsString("Building parent-module")),
                                        oneOf(containsString("Building server-modules")),
                                        oneOf(containsString("Building server-module-a")),
                                        oneOf(containsString("Building server-module-b")),
                                        oneOf(containsString("Building server-module-c"))));
    }

    private void installsAllModulesIntoTheRepoWithTheBuildNumber() throws Exception {
        assertArtifactInLocalRepo(GROUP_ID, "nested-project", expectedAggregatorVersion);
        assertArtifactInLocalRepo(GROUP_ID, "core-utils", expectedCoreVersion);
        assertArtifactInLocalRepo(GROUP_ID, "console-app", expectedAppVersion);
        assertArtifactInLocalRepo(GROUP_ID, "parent-module", expectedParentVersion);
        assertArtifactInLocalRepo(GROUP_ID, "server-modules", expectedServerModulesVersion);
        assertArtifactInLocalRepo(GROUP_ID, "server-module-a", expectedServerModuleAVersion);
        assertArtifactInLocalRepo(GROUP_ID, "server-module-b", expectedServerModuleBVersion);
        assertArtifactInLocalRepo(GROUP_ID + ".misnamed", "server-module-c", expectedServerModuleCVersion);
    }

    private void assertBothReposTagged(String module, String version, String groupSuffix) {
        assertThat(testProject.local, hasTagWithModuleVersion(GROUP_ID + groupSuffix, module, version));
        assertThat(testProject.origin, hasTagWithModuleVersion(GROUP_ID + groupSuffix, module, version));
    }

    private void assertBothReposNotTagged(String module, String version) {
        assertThat(testProject.local, not(hasTagWithModuleVersion(GROUP_ID, module, version)));
        assertThat(testProject.origin, not(hasTagWithModuleVersion(GROUP_ID, module, version)));
    }

    // TODO
    @Ignore("fix for all tests")
    @Test
    public void thePomChangesAreRevertedAfterTheRelease() throws IOException, InterruptedException {
        ObjectId originHeadAtStart = head(testProject.origin);
        ObjectId localHeadAtStart = head(testProject.local);
        assertThat(originHeadAtStart, equalTo(localHeadAtStart));
        testProject.mvnRelease();
        assertThat(head(testProject.origin), equalTo(originHeadAtStart));
        assertThat(head(testProject.local), equalTo(localHeadAtStart));
        assertThat(testProject.local, hasCleanWorkingDirectory());
    }

    private ObjectId head(Git git) throws IOException {
        return git.getRepository().getRef("HEAD").getObjectId();
    }
}
