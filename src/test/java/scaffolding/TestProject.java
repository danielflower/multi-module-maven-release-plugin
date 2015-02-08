package scaffolding;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static scaffolding.MvnRunner.runMaven;
import static scaffolding.Photocopier.copyTestProjectToTemporaryLocation;

public class TestProject {

    public final File originDir;
    public final Git origin;

    public final File localDir;
    public final Git local;

    private TestProject(File originDir, Git origin, File localDir, Git local) {
        this.originDir = originDir;
        this.origin = origin;
        this.localDir = localDir;
        this.local = local;
    }

    /**
     * Runs a mvn command against the local repo and returns the console output.
     */
    public List<String> mvn(String... arguments) throws IOException {
        return runMaven(localDir, arguments);
    }

    public List<String> mvnRelease(String buildNumber) throws IOException, InterruptedException {
        return runMaven(localDir,
            "-DbuildNumber=" + buildNumber,
            "releaser:release");
    }

    public TestProject commitRandomFile(String module) throws IOException, GitAPIException {
        File moduleDir = new File(localDir, module);
        if (!moduleDir.isDirectory()) {
            throw new RuntimeException("Could not find " + moduleDir.getCanonicalPath());
        }
        File random = new File(moduleDir, UUID.randomUUID() + ".txt");
        random.createNewFile();
        local.add().addFilepattern(module + "/" + random.getName()).call();
        local.commit().setMessage("Adding " + random.getName()).call();
        return this;
    }

    public void pushIt() throws GitAPIException {
        local.push().call();
    }

    private static TestProject project(String name) {
        try {
            File originDir = copyTestProjectToTemporaryLocation(name);

            InitCommand initCommand = Git.init();
            initCommand.setDirectory(originDir);
            Git origin = initCommand.call();

            origin.add().addFilepattern(".").call();
            origin.commit().setMessage("Initial commit").call();


            File localDir = Photocopier.folderForSampleProject(name);
            Git local = Git.cloneRepository()
                .setBare(false)
                .setDirectory(localDir)
                .setURI(originDir.toURI().toString())
                .call();

            return new TestProject(originDir, origin, localDir, local);
        } catch (Exception e) {
            throw new RuntimeException("Error while creating copies of the test project");
        }
    }

    public static TestProject singleModuleProject() {
        return project("single-module");
    }

    public static TestProject moduleWithProfilesProject() {
        return project("module-with-profiles");
    }

    public static TestProject inheritedVersionsFromParent() {
        return project("inherited-versions-from-parent");
    }

    public static TestProject independentVersionsProject() {
        return project("independent-versions");
    }

    public static TestProject parentAsSibilngProject() {
        return project("parent-as-sibling");
    }

    public static TestProject moduleWithTestFailure() {
        return project("module-with-test-failure");
    }

}
