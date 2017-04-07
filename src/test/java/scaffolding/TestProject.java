package scaffolding;

import e2e.ProjectType;

import static de.hilling.maven.release.FileUtils.pathOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.isInSynchWithOrigin;
import static scaffolding.Photocopier.copyTestProjectToTemporaryLocation;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.rules.ExternalResource;

import de.hilling.maven.release.TestUtils;

public class TestProject extends ExternalResource {

    private static final MvnRunner DEFAULT_RUNNER;
    private static final String PLUGIN_VERSION_FOR_TESTS = "3-SNAPSHOT";

    static {
        DEFAULT_RUNNER = new MvnRunner(null);
        try {
            MvnRunner.installReleasePluginToLocalRepo();
        } catch (MavenInvocationException e) {
            throw new RuntimeException("unable to install plugin");
        }
    }

    private final String artifactId;
    public        File   originDir;
    public        Git    origin;
    public        File   localDir;
    public        Git    local;
    public  boolean checkClean = true;
    private AtomicInteger commitCounter = new AtomicInteger(1);
    private ProjectType type;
    private MvnRunner   mvnRunner;
    private boolean purge      = true;

    public TestProject(ProjectType type) {
        this.type = type;
        artifactId = type.getSubmoduleName();
        mvnRunner = DEFAULT_RUNNER;
    }

    /**
     * Create initialized and usable project.
     *
     * @param type project test type
     *
     * @return fresh, usable project.
     *
     * @deprecated use with Role to prevent cleaning local repo in the middle of test.
     */
    @Deprecated
    public static TestProject project(ProjectType type) {
        final TestProject testProject = new TestProject(type);
        testProject.purge = false;
        testProject.before();
        return testProject;
    }

    public static void performPomSubstitution(File sourceDir) {
        File pom = new File(sourceDir, "pom.xml");
        if (pom.exists()) {
            try {
                String xml = FileUtils.readFileToString(pom, "UTF-8");
                if (xml.contains("${scm.url}")) {
                    xml = xml.replace("${scm.url}", dirToGitScmReference(sourceDir));
                }
                xml = xml.replace("${current.plugin.version}", PLUGIN_VERSION_FOR_TESTS);
                FileUtils.writeStringToFile(pom, xml, "UTF-8");
            } catch (IOException e) {
                throw new RuntimeException("unable to substitute poms");
            }
        }
        for (File child : sourceDir.listFiles((FileFilter) FileFilterUtils.directoryFileFilter())) {
            performPomSubstitution(child);
        }
    }

    public static String dirToGitScmReference(File sourceDir) {
        return "scm:git:file://localhost/" + pathOf(sourceDir).replace('\\', '/').toLowerCase();
    }

    @Override
    protected void before() {
        final String submoduleName = type.getSubmoduleName();
        final String subfolderName = RandomStringUtils.randomAlphanumeric(10);
        originDir = copyTestProjectToTemporaryLocation(submoduleName, subfolderName);
        performPomSubstitution(originDir);

        InitCommand initCommand = Git.init();
        initCommand.setDirectory(originDir);
        try {
            origin = initCommand.call();

            origin.add().addFilepattern(".").call();
            origin.commit().setMessage("Initial commit").call();

            localDir = Photocopier.folderForSampleProject(submoduleName, subfolderName + "-workdir");
            local = Git.cloneRepository().setBare(false).setDirectory(localDir).setURI(originDir.toURI().toString())
                       .call();
        } catch (GitAPIException e) {
            throw new RuntimeException("error accessing/creating git repo", e);
        }
        if (purge) {
            mvnRun("dependency:purge-local-repository",
                   "-DmanualInclude=de.hilling.maven.release.testprojects:,de.hilling.maven.release.testproject",
                   "-DactTransitively=false");
        }
    }

    @Override
    protected void after() {
        if (checkClean) {
            try {
                origin.reset().setMode(ResetCommand.ResetType.HARD).call();
            } catch (GitAPIException e) {
                throw new RuntimeException("resetting origin failed", e);
            }
            assertThat(local, hasCleanWorkingDirectory());
            assertThat(local, isInSynchWithOrigin());
        }
    }

    /**
     * Runs a mvn command against the local repo and returns the console output.
     */
    public List<String> mvn(String... arguments) throws IOException {
        return mvnRunner.runMaven(localDir, arguments);
    }

    public List<String> mvnRelease(String... arguments) throws IOException, InterruptedException {
        return mvnRun(TestUtils.RELEASE_GOAL, arguments);
    }

    public List<String> mvnReleaseBugfix() throws IOException, InterruptedException {
        return mvnRunner.runMaven(localDir, "-DperformBugfixRelease=true", TestUtils.RELEASE_GOAL);
    }

    public List<String> mvnReleaserNext(String... arguments) throws IOException, InterruptedException {
        return mvnRun(TestUtils.NEXT_GOAL, arguments);
    }

    public TestProject commitFile(String module, String fileName, String fileContent) throws IOException,
                                                                                             GitAPIException {
        File moduleDir = new File(localDir, module);
        if (!moduleDir.isDirectory()) {
            throw new RuntimeException("Could not find " + moduleDir.getCanonicalPath());
        }
        File file = new File(moduleDir, fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileUtils.write(file, fileContent, StandardCharsets.UTF_8);
        String modulePath = module.equals(".")
                            ? ""
                            : module + "/";
        local.add().addFilepattern(modulePath + file.getName()).call();
        local.commit().setMessage("Commit " + commitCounter.getAndIncrement() + ": adding " + file.getName()).call();
        return this;
    }

    public TestProject commitRandomFile(String module) throws IOException, GitAPIException {
        File moduleDir = new File(localDir, module);
        if (!moduleDir.isDirectory()) {
            throw new RuntimeException("Could not find " + moduleDir.getCanonicalPath());
        }
        File random = new File(moduleDir, UUID.randomUUID() + ".txt");
        random.createNewFile();
        String modulePath = module.equals(".")
                            ? ""
                            : module + "/";
        local.add().addFilepattern(modulePath + random.getName()).call();
        local.commit().setMessage("Commit " + commitCounter.getAndIncrement() + ": adding " + random.getName()).call();
        return this;
    }

    public void pushIt() throws GitAPIException {
        local.push().call();
    }

    private List<String> mvnRun(String goal, String... arguments) {
        String[] args = new String[arguments.length + 1];
        System.arraycopy(arguments, 0, args, 1, arguments.length);
        args[0] = goal;
        return mvnRunner.runMaven(localDir, args);
    }

    public void setMvnRunner(MvnRunner mvnRunner) {
        this.mvnRunner = mvnRunner;
    }

    public String getArtifactId() {
        return artifactId;
    }
}
