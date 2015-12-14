package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Test;

import scaffolding.MvnRunner;
import scaffolding.TestProject;

public class SnapshotTest {

    @BeforeClass
    public static void installPluginToLocalRepo()
            throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void allowSnapshotParentsIfFlagIsSet() throws Exception {
        // Install the snapshot parent so that it can be built
        TestProject parent = TestProject.independentVersionsProject();
        parent.mvn("install");
        TestProject project = TestProject.moduleWithAllowSnapshotParents();
        // this should work as the snapshot parent is in the local repo
        project.mvn("install");
        project.mvnRelease("1");
    }

    @Test
    public void allowSpecificSnapshotParents() throws Exception {
        // Install the snapshot parent so that it can be built
        TestProject parent = TestProject.independentVersionsProject();
        parent.mvn("install");
        TestProject project = TestProject.moduleWithAllowedSnapshotParents();
        // this should work as the snapshot parent is in the local repo
        project.mvn("install");
        project.mvnRelease("1");
    }

    @Test
    public void allowSnapshotDependenciesIfFlagIsSet() throws Exception {
        // Install the snapshot dependency so that it can be built
        TestProject dependency = TestProject.independentVersionsProject();
        dependency.mvn("install");
        TestProject project = TestProject.moduleWithAllowSnapshotDependencies();
        // this should work as the snapshot dependency is in the local repo
        project.mvn("install");
        project.mvnRelease("1");
    }

    @Test
    public void allowSpecificSnapshotDependencies() throws Exception {
        // Install the snapshot dependency so that it can be built
        TestProject dependency = TestProject.independentVersionsProject();
        dependency.mvn("install");
        TestProject project = TestProject
                .moduleWithAllowedSnapshotDependencies();
        // this should work as the snapshot dependency is in the local repo
        project.mvn("install");
        project.mvnRelease("1");
    }

    @Test
    public void allowSnapshotPluginsIfFlagIsSet() throws Exception {
        // Install the snapshot plugin so that it can be built
        TestProject plugin = TestProject.testPlugin();
        plugin.mvn("install");
        TestProject project = TestProject.moduleWithAllowSnapshotPlugins();
        // this should work as the snapshot dependency is in the local repo
        project.mvn("install");
        project.mvnRelease("1");
    }

    @Test
    public void allowSpecificSnapshotPlugins() throws Exception {
        // Install the snapshot plugin so that it can be built
        TestProject plugin = TestProject.testPlugin();
        plugin.mvn("install");
        TestProject project = TestProject.moduleWithAllowedSnapshotPlugins();
        // this should work as the snapshot dependency is in the local repo
        project.mvn("install");
        project.mvnRelease("1");
    }

}
