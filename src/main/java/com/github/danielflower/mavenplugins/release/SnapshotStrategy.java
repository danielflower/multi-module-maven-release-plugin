package com.github.danielflower.mavenplugins.release;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

/**
 * Describes how the release goal should handle SNAPSHOT
 * parents/dependencies/plugins.
 * 
 * @author Ronald J. Jenkins Jr.
 */
public class SnapshotStrategy {

    private final boolean allowSnapshotParents;
    private final List<ExemptSnapshotArtifact> allowedSnapshotParents;
    private final boolean allowSnapshotDependencies;
    private final List<ExemptSnapshotArtifact> allowedSnapshotDependencies;
    private final boolean allowSnapshotPlugins;
    private final List<ExemptSnapshotArtifact> allowedSnapshotPlugins;

    /**
     * Constructor.
     * 
     * @param allowSnapshotParents
     *            if true, the SNAPSHOT parent check is skipped.
     * @param allowedSnapshotParents
     *            the SNAPSHOT parent check is skipped for these parents.
     *            Ignored if allowSnapshotParents is true.
     * @param allowSnapshotDependencies
     *            if true, the SNAPSHOT dependency check is skipped.
     * @param allowedSnapshotDependencies
     *            the SNAPSHOT dependency check is skipped for these
     *            dependencies. Ignored if allowSnapshotDependencies is true.
     * @param allowSnapshotPlugins
     *            if true, the SNAPSHOT plugin check is skipped.
     * @param allowedSnapshotPlugins
     *            the SNAPSHOT plugin check is skipped for these plugins.
     *            Ignored if allowSnapshotPlugins is true.
     */
    public SnapshotStrategy(boolean allowSnapshotParents,
            List<ExemptSnapshotArtifact> allowedSnapshotParents,
            boolean allowSnapshotDependencies,
            List<ExemptSnapshotArtifact> allowedSnapshotDependencies,
            boolean allowSnapshotPlugins,
            List<ExemptSnapshotArtifact> allowedSnapshotPlugins) {
        this.allowSnapshotParents = allowSnapshotParents;
        this.allowedSnapshotParents = clean(allowedSnapshotParents,
                allowSnapshotParents);
        this.allowSnapshotDependencies = allowSnapshotDependencies;
        this.allowedSnapshotDependencies = clean(allowedSnapshotDependencies,
                allowSnapshotDependencies);
        this.allowSnapshotPlugins = allowSnapshotPlugins;
        this.allowedSnapshotPlugins = clean(allowedSnapshotPlugins,
                allowSnapshotPlugins);
    }

    /**
     * Indicates whether or not the given parent is allowed to have a SNAPSHOT
     * version.
     * 
     * @param groupId
     *            the group ID of the parent.
     * @param artifactId
     *            the artifact ID of the parent.
     * @return true if the parent is allowed to have a SNAPSHOT version, false
     *         otherwise.
     */
    public boolean allowParent(MavenProject parent) {
        return this.allowSnapshotParents
                || this.allowedSnapshotParents
                        .contains(new ExemptSnapshotArtifact(parent
                                .getGroupId(), parent.getArtifactId()));

    }

    /**
     * Indicates whether or not the given dependency is allowed to have a
     * SNAPSHOT version.
     * 
     * @param groupId
     *            the group ID of the dependency.
     * @param artifactId
     *            the artifact ID of the dependency.
     * @return true if the dependency is allowed to have a SNAPSHOT version,
     *         false otherwise.
     */
    public boolean allowDependency(Dependency dependency) {
        return this.allowSnapshotDependencies
                || this.allowedSnapshotDependencies
                        .contains(new ExemptSnapshotArtifact(dependency
                                .getGroupId(), dependency.getArtifactId()));

    }

    /**
     * Indicates whether or not the given plugin is allowed to have a SNAPSHOT
     * version.
     * 
     * @param groupId
     *            the group ID of the plugin.
     * @param artifactId
     *            the artifact ID of the plugin.
     * @return true if the plugin is allowed to have a SNAPSHOT version, false
     *         otherwise.
     */
    public boolean allowPlugin(Plugin plugin) {
        return this.allowSnapshotPlugins
                || this.allowedSnapshotPlugins
                        .contains(new ExemptSnapshotArtifact(plugin
                                .getGroupId(), plugin.getArtifactId()));

    }

    /**
     * Cleanses an input list of artifacts.
     * 
     * @param list
     *            see return description.
     * @param empty
     *            see return description.
     * @return if empty is true or if the list is null or empty, an empty list
     *         is returned. Otherwise, a copy of the input list is returned.
     *         Never null.
     */
    private static List<ExemptSnapshotArtifact> clean(
            List<ExemptSnapshotArtifact> list, boolean empty) {
        if (empty || list == null || list.size() == 0) {
            return Collections.emptyList();
        }
        return new ArrayList<ExemptSnapshotArtifact>(list);
    }

}
