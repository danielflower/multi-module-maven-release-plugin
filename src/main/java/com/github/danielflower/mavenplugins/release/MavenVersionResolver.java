package com.github.danielflower.mavenplugins.release;

import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

public class MavenVersionResolver {

    public static void resolveVersionsDefinedThroughProperties(List<MavenProject> projects) {
        for (MavenProject project : projects) {
            if (isVersionDefinedWithProperty(project.getVersion())) {
                project.setVersion(resolveVersion(project.getVersion(), project.getProperties()));
            }
        }

    }

    public static String resolveVersion(String version, Properties projectProperties) {
        if (isVersionDefinedWithProperty(version)) {
            return projectProperties.getProperty(version.replace("${", "").replace("}", ""), version);
        }
        return version;
    }

    private static boolean isVersionDefinedWithProperty(String version) {
        return version != null && version.startsWith("${");
    }

    public static boolean isMultiModuleReleasePlugin(Plugin plugin) {
        return plugin.getGroupId().equals("com.github.danielflower.mavenplugins") && plugin.getArtifactId().equals("multi-module-maven-release-plugin");
    }

    public static boolean isSnapshot(String version) {
        return (version != null && version.endsWith("-SNAPSHOT"));
    }
}
