package e2e;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Before;
import org.junit.Test;
import scaffolding.MvnRunner;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class SingleModuleTest {

    @Before
    public void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void canDoIt() throws IOException, InterruptedException {
        File projectDir = copyTestProjectToTemporaryLocation("test-project-single-module");
        MvnRunner.runReleaseOn(projectDir);
    }

    private File copyTestProjectToTemporaryLocation(String moduleName) throws IOException {
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
