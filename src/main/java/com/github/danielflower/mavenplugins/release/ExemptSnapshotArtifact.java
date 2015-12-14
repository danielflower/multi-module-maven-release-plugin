package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * An artifact that the release goal should ignore if its version is a SNAPSHOT
 * version.
 * 
 * @author Ronald J. Jenkins Jr.
 */
public class ExemptSnapshotArtifact {

    @Parameter(property = "groupId")
    private String groupId;

    @Parameter(property = "artifactId")
    private String artifactId;

    /** Maven constructor. */
    public ExemptSnapshotArtifact() {
    }

    /**
     * Constructor.
     * 
     * @param groupId
     *            the group ID of the artifact.
     * @param artifactId
     *            the artifact ID of the artifact.
     */
    /* package */ExemptSnapshotArtifact(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    /**
     * Returns the artifact ID of the artifact.
     * 
     * @return not null.
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Returns the group ID of the artifact.
     * 
     * @return not null.
     */
    public String getGroupId() {
        return groupId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof ExemptSnapshotArtifact))
            return false;
        ExemptSnapshotArtifact esa = (ExemptSnapshotArtifact) obj;
        if (!esa.getGroupId().equals(this.getGroupId()))
            return false;
        if (!esa.getArtifactId().equals(this.getArtifactId()))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + this.getGroupId().hashCode();
        result = 31 * result + this.getArtifactId().hashCode();
        return result;
    }

}
