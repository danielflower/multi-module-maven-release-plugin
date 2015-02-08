package com.github.danielflower.mavenplugins.release;

import java.io.File;
import java.io.IOException;

public class FileUtils {
    public static String pathOf(File file) {
        String path;
        try {
            path = file.getCanonicalPath();
        } catch (IOException e1) {
            path = file.getAbsolutePath();
        }
        return path;
    }
}
