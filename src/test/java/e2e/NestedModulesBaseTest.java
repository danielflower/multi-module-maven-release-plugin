package e2e;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.hasTag;

import java.io.IOException;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Test;

import scaffolding.TestProject;

public abstract class NestedModulesBaseTest extends E2ETest {

	final String expectedAggregatorVersion = "0.0.";
	final String expectedParentVersion = "1.2.3.";
	final String expectedCoreVersion = "2.0.";
	final String expectedAppVersion = "3.2.";
	final String expectedServerModulesVersion = "1.0.2.4.";
	final String expectedServerModuleAVersion = "3.0.";
	final String expectedServerModuleBVersion = "3.1.";
	final String expectedServerModuleCVersion = "3.2.";

	final TestProject testProject = newTestProject();
	private final RepositorySystem system = newRepositorySystem();

	protected abstract TestProject newTestProject();

	private RepositorySystem newRepositorySystem() {
		/*
		 * Aether's components implement org.eclipse.aether.spi.locator.Service
		 * to ease manual wiring and using the prepopulated
		 * DefaultServiceLocator, we only need to register the repository
		 * connector and transporter factories.
		 */
		final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);

		locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
			@Override
			public void serviceCreationFailed(final Class<?> type, final Class<?> impl, final Throwable exception) {
				exception.printStackTrace();
			}
		});

		return locator.getService(RepositorySystem.class);
	}

	private DefaultRepositorySystemSession newRepositorySystemSession() {
		final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		final LocalRepository localRepo = new LocalRepository("maven-repo");
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
		session.setOffline(true);
		return session;
	}

	private Artifact consoleApp(final String buildNumber) {
		return project("console-app", expectedAppVersion, buildNumber);
	}

	private Artifact serverModuleB(final String buildNumber) {
		return project("server-module-b", expectedServerModuleBVersion, buildNumber);
	}

	private Artifact serverModuleC(final String buildNumber) {
		return misnamedProject("server-module-c", expectedServerModuleCVersion, buildNumber);
	}

	@Test
	public void buildsAndInstallsAndTagsAllModules() throws Exception {
		buildsEachProjectOnceAndOnlyOnce(testProject.mvnRelease("1"));
		installsAllModulesIntoTheRepoWithTheBuildNumber();

		assertBothReposTagged("nested-project", expectedAggregatorVersion, "1");
		assertBothReposTagged("core-utils", expectedCoreVersion, "1");
		assertBothReposTagged("console-app", expectedAppVersion, "1");
		assertDependencyUpdated(consoleApp("1"), "core-utils", expectedCoreVersion, "1");
		assertDependencyUpdated(consoleApp("1"), "server-module-c", expectedServerModuleCVersion, "1");
		assertBothReposTagged("parent-module", expectedParentVersion, "1");
		assertBothReposTagged("server-modules", expectedServerModulesVersion, "1");
		assertBothReposTagged("server-module-a", expectedServerModuleAVersion, "1");
		assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "1");
		assertDependencyUpdated(serverModuleB("1"), "server-module-a", expectedServerModuleAVersion, "1");
		assertBothReposTagged("server-module-c", expectedServerModuleCVersion, "1");
		assertDependencyUpdated(serverModuleC("1"), "server-module-b", expectedServerModuleBVersion, "1");

		testProject.commitRandomFile("server-modules/server-module-b");
		testProject.mvn("releaser:release");

		assertBothReposNotTagged("nested-project", expectedAggregatorVersion, "2");
		assertBothReposNotTagged("core-utils", expectedCoreVersion, "2");
		assertBothReposTagged("console-app", expectedAppVersion, "2");
		assertDependencyUpdated(consoleApp("2"), "core-utils", expectedCoreVersion, "1");
		assertDependencyUpdated(consoleApp("2"), "server-module-c", expectedServerModuleCVersion, "2");
		assertBothReposNotTagged("parent-module", expectedParentVersion, "2");
		assertBothReposNotTagged("server-modules", expectedServerModulesVersion, "2");
		assertBothReposNotTagged("server-module-a", expectedServerModuleAVersion, "2");
		assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "2");
		assertDependencyUpdated(serverModuleB("2"), "server-module-a", expectedServerModuleAVersion, "1");
		assertBothReposTagged("server-module-c", expectedServerModuleCVersion, "2");
		assertDependencyUpdated(serverModuleC("2"), "server-module-b", expectedServerModuleBVersion, "2");

		testProject.commitRandomFile("parent-module");
		testProject.mvn("releaser:release");

		assertBothReposNotTagged("nested-project", expectedAggregatorVersion, "2");
		assertBothReposTagged("core-utils", expectedCoreVersion, "2");
		assertBothReposTagged("console-app", expectedAppVersion, "3");
		assertDependencyUpdated(consoleApp("3"), "core-utils", expectedCoreVersion, "2");
		assertDependencyUpdated(consoleApp("3"), "server-module-c", expectedServerModuleCVersion, "3");
		assertBothReposTagged("parent-module", expectedParentVersion, "2");
		assertBothReposNotTagged("server-modules", expectedServerModulesVersion, "3");
		assertBothReposTagged("server-module-a", expectedServerModuleAVersion, "2");
		assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "3");
		assertDependencyUpdated(serverModuleB("3"), "server-module-a", expectedServerModuleAVersion, "2");
		assertBothReposTagged("server-module-c", expectedServerModuleCVersion, "3");
		assertDependencyUpdated(serverModuleC("3"), "server-module-b", expectedServerModuleBVersion, "3");

		testProject.mvnRelease("4");
		assertBothReposTagged("nested-project", expectedAggregatorVersion, "4");
		assertBothReposTagged("core-utils", expectedCoreVersion, "4");
		assertBothReposTagged("console-app", expectedAppVersion, "4");
		assertDependencyUpdated(consoleApp("4"), "core-utils", expectedCoreVersion, "4");
		assertDependencyUpdated(consoleApp("4"), "server-module-c", expectedServerModuleCVersion, "4");
		assertBothReposTagged("parent-module", expectedParentVersion, "4");
		assertBothReposTagged("server-modules", expectedServerModulesVersion, "4");
		assertBothReposTagged("server-module-a", expectedServerModuleAVersion, "4");
		assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "4");
		assertDependencyUpdated(serverModuleB("4"), "server-module-a", expectedServerModuleAVersion, "4");
		assertBothReposTagged("server-module-c", expectedServerModuleCVersion, "4");
		assertDependencyUpdated(serverModuleC("4"), "server-module-b", expectedServerModuleBVersion, "4");
	}

	private Artifact project(final String artifacdId, final String version, final String buildNumber) {
		return new DefaultArtifact(format("com.github.danielflower.mavenplugins.testprojects.nested:%s:%s%s",
				artifacdId, version, buildNumber));
	}

	private Artifact misnamedProject(final String artifactId, final String version, final String buildNumber) {
		return new DefaultArtifact(format("com.github.danielflower.mavenplugins.testprojects.nested.misnamed:%s:%s%s",
				artifactId, version, buildNumber));
	}

	private void assertDependencyUpdated(final Artifact artifact, final String targetProject,
			final String expectedVersion, final String expectedBuildNumber) throws Exception {
		final RepositorySystemSession session = newRepositorySystemSession();
		final ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
		descriptorRequest.setArtifact(artifact);

		final ArtifactDescriptorResult descriptorResult = system.readArtifactDescriptor(session, descriptorRequest);

		Dependency targetDependency = null;
		for (final Dependency dep : descriptorResult.getDependencies()) {
			if (targetProject.equals(dep.getArtifact().getArtifactId())) {
				targetDependency = dep;
				break;
			}
		}

		assertNotNull(format("No dependency found with artifact-id %s", targetProject), targetDependency);
		assertEquals(format("%s%s", expectedVersion, expectedBuildNumber), targetDependency.getArtifact().getVersion());
	}

	@SuppressWarnings("unchecked")
	private void buildsEachProjectOnceAndOnlyOnce(final List<String> commandOutput) throws Exception {
		assertThat(commandOutput,
				allOf( /* once for initial build; once for release build */ twoOf(
						containsString("Building nested-project")), oneOf(containsString("Building core-utils")),
				oneOf(containsString("Building console-app")), oneOf(containsString("Building parent-module")),
				oneOf(containsString("Building server-modules")), oneOf(containsString("Building server-module-a")),
				oneOf(containsString("Building server-module-b")), oneOf(containsString("Building server-module-c"))));
	}

	private void installsAllModulesIntoTheRepoWithTheBuildNumber() throws Exception {
		assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "nested-project",
				expectedAggregatorVersion + "1");
		assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "core-utils",
				expectedCoreVersion + "1");
		assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "console-app",
				expectedAppVersion + "1");
		assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "parent-module",
				expectedParentVersion + "1");
		assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "server-modules",
				expectedServerModulesVersion + "1");
		assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "server-module-a",
				expectedServerModuleAVersion + "1");
		assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested", "server-module-b",
				expectedServerModuleBVersion + "1");
		assertArtifactInLocalRepo("com.github.danielflower.mavenplugins.testprojects.nested.misnamed",
				"server-module-c", expectedServerModuleCVersion + "1");
	}

	private void assertBothReposTagged(final String module, final String version, final String buildNumber) {
		final String tag = module + "-" + version + buildNumber;
		assertThat(testProject.local, hasTag(tag));
		assertThat(testProject.origin, hasTag(tag));
	}

	private void assertBothReposNotTagged(final String module, final String version, final String buildNumber) {
		final String tag = module + "-" + version + buildNumber;
		assertThat(testProject.local, not(hasTag(tag)));
		assertThat(testProject.origin, not(hasTag(tag)));
	}

	@Test
	public void thePomChangesAreRevertedAfterTheRelease() throws IOException, InterruptedException {
		final ObjectId originHeadAtStart = head(testProject.origin);
		final ObjectId localHeadAtStart = head(testProject.local);
		assertThat(originHeadAtStart, equalTo(localHeadAtStart));
		testProject.mvnRelease("1");
		assertThat(head(testProject.origin), equalTo(originHeadAtStart));
		assertThat(head(testProject.local), equalTo(localHeadAtStart));
		assertThat(testProject.local, hasCleanWorkingDirectory());
	}

	private ObjectId head(final Git git) throws IOException {
		return git.getRepository().getRef("HEAD").getObjectId();
	}
}
