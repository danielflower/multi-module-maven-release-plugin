package scaffolding;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class Photocopier {
    public static File copyTestProjectToTemporaryLocation(String moduleName, String subfolder)  {
        File source = new File("test-projects", moduleName);
        if (!source.isDirectory()) {
            source = new File(FilenameUtils.separatorsToSystem("../test-projects/" + moduleName));
        }
        if (!source.isDirectory()) {
            throw new RuntimeException("Could not find module " + moduleName);
        }

        File target = folderForSampleProject(moduleName, subfolder);
        try {
            FileUtils.copyDirectory(source, target);
        } catch (IOException e) {
            throw new RuntimeException("unable to copy project", e);
        }
        return target;
    }

    public static File folderForSampleProject(String moduleName, String subfolder) {
        return new File(FilenameUtils.separatorsToSystem("target/samples/" + moduleName + "/" + subfolder));
    }
}
