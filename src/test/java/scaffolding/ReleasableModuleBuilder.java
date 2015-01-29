package scaffolding;

import com.github.danielflower.mavenplugins.release.Clock;
import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.VersionNamer;
import org.apache.maven.project.MavenProject;

public class ReleasableModuleBuilder {

    private final VersionNamer versionNamer = new VersionNamer(Clock.SystemClock);
    MavenProject project = new MavenProject();
    private String buildNumber = "123";

    public ReleasableModuleBuilder withBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
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
        return new ReleasableModule(project, buildNumber, versionNamer);
    }

    public static ReleasableModuleBuilder aModule() {
        return new ReleasableModuleBuilder()
            .withGroupId("com.github.danielflower.somegroup")
            .withArtifactId("some-artifact");
    }
}
