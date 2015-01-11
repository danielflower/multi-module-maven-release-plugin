package scaffolding;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MvnRunner {
    public static boolean haveInstalledPlugin = false;

    public static void installReleasePluginToLocalRepo() throws MavenInvocationException {
        if (haveInstalledPlugin) {
            return;
        }
        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Collections.singletonList("install"));

        Properties props = new Properties();
        props.setProperty("skipTests", "true");
        request.setProperties(props);
        Invoker invoker = new DefaultInvoker();
        CollectingLogOutputStream logOutput = new CollectingLogOutputStream(false);
        invoker.setOutputHandler(new PrintStreamHandler(new PrintStream(logOutput), true));
        InvocationResult result = invoker.execute(request);

        if (result.getExitCode() != 0) {
            for (String line : logOutput.getLines()) {
                System.out.println(line);
            }
        }

        assertThat("Exit code from running mvn install on this project", result.getExitCode(), is(0));
        haveInstalledPlugin = true;
    }

    public static List<String> runReleaseOn(File projectDir, String releaseVersion) throws IOException, InterruptedException {
        return runMaven(projectDir,
            "-DreleaseVersion=" + releaseVersion,
            "multi-module-release:release");
    }

    public static List<String> runMaven(File projectDir, String... arguments) throws IOException {
        String mvnPath = System.getenv("M2_HOME");
        if (StringUtils.isBlank(mvnPath)) {
            throw new RuntimeException("M2_HOME is not set");
        }
        File m2Bin = new File(mvnPath, "bin");
        File m2 = new File(m2Bin, SystemUtils.IS_OS_WINDOWS ? "mvn.bat" : "mvn");
        if (!m2.isFile()) {
            throw new RuntimeException("Could not locate the mvn executable. Looked at " + m2.getCanonicalPath());
        }

        CommandLine command = new CommandLine(m2.getCanonicalPath());
        for (String argument : arguments) {
            command.addArgument(argument, false);
        }

        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(projectDir);

        CollectingLogOutputStream logCollector = new CollectingLogOutputStream(true);
        PumpStreamHandler streamHandler = new PumpStreamHandler(logCollector);
        executor.setStreamHandler(streamHandler);

        ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
        executor.setWatchdog(watchdog);
        int exitCode = executor.execute(command);
        if (exitCode != 0) {
            throw new RuntimeException("Exit code is " + 0);
        }

        return logCollector.getLines();
    }

    public static void assertArtifactInLocalRepo(String groupId, String artifactId, String version) throws IOException, MavenInvocationException {
        String artifact = groupId + ":" + artifactId + ":" + version;
        File temp = new File("target/downloads/" + UUID.randomUUID());

        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Collections.singletonList("org.apache.maven.plugins:maven-dependency-plugin:2.8:copy"));

        Properties props = new Properties();
        props.setProperty("artifact", artifact);
        props.setProperty("outputDirectory", temp.getCanonicalPath());

        request.setProperties(props);
        Invoker invoker = new DefaultInvoker();
        CollectingLogOutputStream logOutput = new CollectingLogOutputStream(false);
        invoker.setOutputHandler(new PrintStreamHandler(new PrintStream(logOutput), true));
        InvocationResult result = invoker.execute(request);

        if (result.getExitCode() != 0) {
            for (String line : logOutput.getLines()) {
                System.out.println(line);
            }
        }

        assertThat("Could not find artifact " + artifact + " in repository", result.getExitCode(), is(0));
    }
}
