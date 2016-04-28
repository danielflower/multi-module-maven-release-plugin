package scaffolding;

import static java.lang.String.format;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.Files.createDirectories;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.SystemUtils.USER_DIR;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;

public class MvnRunner {
	public static final Path WORK_DIRECTORY = getDefault().getPath(USER_DIR);
	public static final Path LOCAL_MAVEN_REPO = WORK_DIRECTORY.resolve("maven-repo");

	static {
		try {
			final long start = System.currentTimeMillis();
			System.out.println("Installing the plugin into the local repo");

			assertThat("Environment variable M2_HOME must be set", System.getenv("M2_HOME") != null);

			final InvocationRequest request = createRequest();
			request.setDebug(true);
			request.setGoals(Collections.singletonList("install"));
			request.getProperties().setProperty("skipTests", "true");

			final Invoker invoker = new DefaultInvoker();
			final CollectingLogOutputStream logOutput = new CollectingLogOutputStream(false);
			invoker.setOutputHandler(new PrintStreamHandler(new PrintStream(logOutput), true));
			final InvocationResult result = invoker.execute(request);

			if (result.getExitCode() != 0) {
				for (final String line : logOutput.getLines()) {
					System.out.println("        " + line);
				}
			}

			assertThat("Exit code from running mvn install on this project", result.getExitCode(), is(0));
			System.out.println("Finished installing the plugin into the local repo in "
					+ (System.currentTimeMillis() - start) + "ms");
		} catch (final Exception th) {
			th.printStackTrace();
			throw new InstantiationError(th.getMessage());
		}
	}

	private static InvocationRequest createRequest() throws IOException {
		final InvocationRequest request = new DefaultInvocationRequest();
		request.setLocalRepositoryDirectory(createDirectories(LOCAL_MAVEN_REPO).toFile());
		final Properties props = new Properties();
		props.setProperty("stacktraceEnabled", "true");
		props.setProperty("localMavenRepo", LOCAL_MAVEN_REPO.toString());
		request.setProperties(props);
		return request;
	}

	public List<String> runMaven(final File workingDir, final String... arguments) throws IOException {
		final InvocationRequest request = createRequest();

		request.setGoals(asList(arguments));
		request.setBaseDirectory(workingDir);

		final Invoker invoker = new DefaultInvoker();
		final CollectingLogOutputStream logOutput = new CollectingLogOutputStream(false);
		invoker.setOutputHandler(new PrintStreamHandler(new PrintStream(logOutput), true));

		int exitCode;
		try {
			final InvocationResult result = invoker.execute(request);
			exitCode = result.getExitCode();
		} catch (final Exception e) {
			throw new MavenExecutionException(1, logOutput.getLines());
		}
		final List<String> output = logOutput.getLines();

		if (exitCode != 0) {
			throw new MavenExecutionException(exitCode, output);
		}

		return output;
	}

	public void assertArtifactInLocalRepo(final String groupId, final String artifactId, final String version)
			throws IOException, MavenInvocationException {
		final String artifact = groupId + ":" + artifactId + ":" + version + ":pom";
		final InvocationRequest request = createRequest();
		request.setGoals(Collections.singletonList("org.apache.maven.plugins:maven-dependency-plugin:2.10:get"));

		final Properties props = new Properties();
		props.setProperty("artifact", artifact);

		request.setProperties(props);
		final Invoker invoker = new DefaultInvoker();
		final CollectingLogOutputStream logOutput = new CollectingLogOutputStream(false);
		invoker.setOutputHandler(new PrintStreamHandler(new PrintStream(logOutput), true));
		final InvocationResult result = invoker.execute(request);

		if (result.getExitCode() != 0) {
			System.out.println();
			System.out.println(
					"There was a problem checking for the existence of the artifact. Here is the output of the mvn command:");
			System.out.println();
			for (final String line : logOutput.getLines()) {
				System.out.println(line);
			}
		}

		assertThat(format("Could not find artifact %s:%s in repository", artifact, "pom"), result.getExitCode(), is(0));
	}

}
