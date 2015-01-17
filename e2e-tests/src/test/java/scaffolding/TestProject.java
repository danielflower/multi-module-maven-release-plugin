package scaffolding;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    public List<String> mvnRelease(String releaseVersion) throws IOException, InterruptedException {
        return runMaven(localDir,
            "-DreleaseVersion=" + releaseVersion,
            "multi-module-release:release");
    }

    private static TestProject project(String name) throws IOException, GitAPIException {
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
    }

    public static TestProject singleModuleProject() throws IOException, GitAPIException {
        return project("single-module");
    }

}
