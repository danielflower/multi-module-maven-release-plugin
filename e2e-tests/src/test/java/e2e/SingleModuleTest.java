package e2e;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SingleModuleTest {

    @Test
    public void canDoIt() throws IOException, InterruptedException {
        File projectDir = prepare("test-project-single-module");
        runReleaseOn(projectDir);
    }

    private void runReleaseOn(File projectDir) throws IOException, InterruptedException {
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
        command.addArgument("com.github.danielflower.mavenplugins:multi-module-release-plugin:1.0-SNAPSHOT:release");

        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
        executor.setWatchdog(watchdog);
        int exitCode = executor.execute(command);

        assertThat(exitCode, is(0));
    }

    private File prepare(String moduleName) throws IOException {
        File source = new File(moduleName);
        if (!source.isDirectory()) {
            source = new File(FilenameUtils.separatorsToSystem("../" + moduleName));
        }
        if (!source.isDirectory()) {
            throw new RuntimeException("Could not find module " + moduleName);
        }

        File target = new File(FilenameUtils.separatorsToSystem("target/samples/" + moduleName + "/" + UUID.randomUUID()));
        FileUtils.copyDirectory(source, target);
        return target;
    }


}
