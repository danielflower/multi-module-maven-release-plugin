package com.github.danielflower.mavenplugins.release;

import java.util.List;
import java.util.Properties;

import org.apache.maven.project.MavenProject;

class MavenVersionResolver {

    static void resolveVersionsDefinedThroughProperties(List<MavenProject> projects) {
        for (MavenProject project : projects) {
            if (isVersionDefinedWithProperty(project.getVersion())) {
                project.setVersion(resolveVersion(project.getVersion(), project.getProperties()));
            }
        }

    }

    static String resolveVersion(String version, Properties projectProperties) {
        if (isVersionDefinedWithProperty(version)) {
            return projectProperties.getProperty(version.replace("${", "").replace("}", ""), version);
        }
        return version;
    }

    static boolean isSnapshot(String version) {
        return (version != null && version.endsWith("-SNAPSHOT"));
    }

    private static boolean isVersionDefinedWithProperty(String version) {
        return version != null && version.startsWith("${");
    }
}
