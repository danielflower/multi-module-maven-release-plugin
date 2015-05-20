package scaffolding;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.danielflower.mavenplugins.release.FileUtils.pathOf;
import static scaffolding.MvnRunner.runMaven;
import static scaffolding.Photocopier.copyTestProjectToTemporaryLocation;

public class TestProject {

    public static final String PLUGIN_VERSION_FOR_TESTS = "1.1-SNAPSHOT";
    public final File originDir;
    public final Git origin;

    public final File localDir;
    public final Git local;

    private final AtomicInteger commitCounter = new AtomicInteger(1);

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

    public List<String> mvnRelease(String buildNumber, String moduleToRelease) throws IOException, InterruptedException {
        return runMaven(localDir,
            "-DbuildNumber=" + buildNumber,
            "-DmodulesToRelease=" + moduleToRelease,
            "releaser:release");
    }

    public TestProject commitRandomFile(String module) throws IOException, GitAPIException {
        File moduleDir = new File(localDir, module);
        if (!moduleDir.isDirectory()) {
            throw new RuntimeException("Could not find " + moduleDir.getCanonicalPath());
        }
        File random = new File(moduleDir, UUID.randomUUID() + ".txt");
        random.createNewFile();
        String modulePath = module.equals(".") ? "" : module + "/";
        local.add().addFilepattern(modulePath + random.getName()).call();
        local.commit().setMessage("Commit " + commitCounter.getAndIncrement() + ": adding " + random.getName()).call();
        return this;
    }

    public void pushIt() throws GitAPIException {
        local.push().call();
    }

    private static TestProject project(String name) {
        try {
            File originDir = copyTestProjectToTemporaryLocation(name);
            performPomSubstitution(originDir);

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
            throw new RuntimeException("Error while creating copies of the test project", e);
        }
    }

    public static void performPomSubstitution(File sourceDir) throws IOException {
        File pom = new File(sourceDir, "pom.xml");
        if (pom.exists()) {
            String xml = FileUtils.readFileToString(pom, "UTF-8");
            if (xml.contains("${scm.url}")) {
                xml = xml.replace("${scm.url}", dirToGitScmReference(sourceDir));
            }
            xml = xml.replace("${current.plugin.version}", PLUGIN_VERSION_FOR_TESTS);
            FileUtils.writeStringToFile(pom, xml, "UTF-8");
        }
        for (File child : sourceDir.listFiles((FileFilter)FileFilterUtils.directoryFileFilter())) {
            performPomSubstitution(child);
        }
    }

    public static String dirToGitScmReference(File sourceDir) {
        return "scm:git:file://localhost/" + pathOf(sourceDir).replace('\\', '/').toLowerCase();
    }

    public static TestProject singleModuleProject() {
        return project("single-module");
    }

    public static TestProject nestedProject() {
        return project("nested-project");
    }

    public static TestProject moduleWithScmTag() {
        return project("module-with-scm-tag");
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
    public static TestProject deepDependenciesProject() {
        return project("deep-dependencies");
    }

    public static TestProject moduleWithTestFailure() {
        return project("module-with-test-failure");
    }
    public static TestProject moduleWithSnapshotDependencies() {
        return project("snapshot-dependencies");
    }

}
