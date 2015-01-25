package scaffolding;

import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.ValidationException;
import org.apache.maven.project.MavenProject;

public class ReleasableModuleBuilder {

    MavenProject project = new MavenProject();
    private String releaseVersion = "123";

    public ReleasableModuleBuilder withReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
        return this;
    }

    public ReleasableModuleBuilder withGroupId(String groupId) {
        project.setGroupId(groupId);
        return this;
    }

    public ReleasableModuleBuilder withArtifactId(String artifactId) {
        project.setArtifactId(artifactId);
        return this;
    }

    public ReleasableModuleBuilder withSnapshotVersion(String snapshotVersion) {
        project.setVersion(snapshotVersion);
        return this;
    }

    public ReleasableModule build() throws ValidationException {
        return new ReleasableModule(project, releaseVersion);
    }

    public static ReleasableModuleBuilder aModule() {
        return new ReleasableModuleBuilder()
            .withGroupId("com.github.danielflower.somegroup")
            .withArtifactId("some-artifact");
    }
}
