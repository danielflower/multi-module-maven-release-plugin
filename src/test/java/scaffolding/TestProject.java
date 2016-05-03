package scaffolding;

import static com.github.danielflower.mavenplugins.release.FileUtils.pathOf;
import static scaffolding.Photocopier.copyTestProjectToTemporaryLocation;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import e2e.E2ETest;

public class TestProject {

	public static final String PLUGIN_VERSION_FOR_TESTS = "2.0-SNAPSHOT";
	public final File originDir;
	public final Git origin;

	public final File localDir;
	public final Git local;

	private final AtomicInteger commitCounter = new AtomicInteger(1);

	private TestProject(final File originDir, final Git origin, final File localDir, final Git local) {
		this.originDir = originDir;
		this.origin = origin;
		this.localDir = localDir;
		this.local = local;
	}

	/**
	 * Runs a mvn command against the local repo and returns the console output.
	 */
	public List<String> mvn(final String... arguments) throws IOException {
		return E2ETest.mvn.runMaven(localDir, arguments);
	}

	public List<String> mvnRelease(final String buildNumber) throws IOException, InterruptedException {
		return E2ETest.mvn.runMaven(localDir, "-DbuildNumber=" + buildNumber, "releaser:release");
	}

	public List<String> mvnReleaserNext(final String buildNumber) throws IOException, InterruptedException {
		return E2ETest.mvn.runMaven(localDir, "-DbuildNumber=" + buildNumber, "releaser:next");
	}

	public List<String> mvnRelease(final String buildNumber, final String moduleToRelease)
			throws IOException, InterruptedException {
		return E2ETest.mvn.runMaven(localDir, "-DbuildNumber=" + buildNumber, "-DmodulesToRelease=" + moduleToRelease,
				"releaser:release");
	}

	public TestProject commitRandomFile(final String module) throws IOException, GitAPIException {
		final File moduleDir = new File(localDir, module);
		if (!moduleDir.isDirectory()) {
			throw new RuntimeException("Could not find " + moduleDir.getCanonicalPath());
		}
		final File random = new File(moduleDir, UUID.randomUUID() + ".txt");
		random.createNewFile();
		final String modulePath = module.equals(".") ? "" : module + "/";
		local.add().addFilepattern(modulePath + random.getName()).call();
		local.commit().setMessage("Commit " + commitCounter.getAndIncrement() + ": adding " + random.getName()).call();
		return this;
	}

	public void pushIt() throws GitAPIException {
		local.push().call();
	}

	private static TestProject project(final String name) {
		try {
			final File originDir = copyTestProjectToTemporaryLocation(name);
			performPomSubstitution(originDir);

			final InitCommand initCommand = Git.init();
			initCommand.setDirectory(originDir);
			final Git origin = initCommand.call();

			origin.add().addFilepattern(".").call();
			origin.commit().setMessage("Initial commit").call();

			final File localDir = Photocopier.folderForSampleProject(name);
			final Git local = Git.cloneRepository().setBare(false).setDirectory(localDir)
					.setURI(originDir.toURI().toString()).call();

			return new TestProject(originDir, origin, localDir, local);
		} catch (final Exception e) {
			throw new RuntimeException("Error while creating copies of the test project", e);
		}
	}

	public static void performPomSubstitution(final File sourceDir) throws IOException {
		final File pom = new File(sourceDir, "pom.xml");
		if (pom.exists()) {
			String xml = FileUtils.readFileToString(pom, "UTF-8");
			if (xml.contains("${scm.url}")) {
				xml = xml.replace("${scm.url}", dirToGitScmReference(sourceDir));
			}
			xml = xml.replace("${current.plugin.version}", PLUGIN_VERSION_FOR_TESTS);
			FileUtils.writeStringToFile(pom, xml, "UTF-8");
		}
		for (final File child : sourceDir.listFiles((FileFilter) FileFilterUtils.directoryFileFilter())) {
			performPomSubstitution(child);
		}
	}

	public static String dirToGitScmReference(final File sourceDir) {
		return "scm:git:file://localhost/" + pathOf(sourceDir).replace('\\', '/');
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
