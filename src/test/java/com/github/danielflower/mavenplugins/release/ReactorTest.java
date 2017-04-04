package com.github.danielflower.mavenplugins.release;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.versioning.ImmutableModuleVersion;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableQualifiedArtifact;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseDateSingleton;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;

public class ReactorTest {

    public static final String GROUP_ID = "my.great.group";
    public static final String ARTIFACT_ID = "some-arty";
    private ReleaseInfo previousRelease;
    private int         number;

    @Before
    public void setUp() {
        number = 0;
        previousRelease = TestUtils.releaseInfo(2, 4, "testtag", ARTIFACT_ID);
    }

    @Test
    public void canFindModulesByGroupAndArtifactName() throws Exception {
        final MavenProject project = new MavenProject();
        project.setGroupId(GROUP_ID);
        project.setArtifactId(ARTIFACT_ID);
        ReleasableModule arty = createModule().build();
        Reactor reactor = new Reactor(asList(createModule().build(), arty, createModule().build()));
        assertThat(reactor.find(GROUP_ID, "some-arty-0"), is(arty));
        assertThat(reactor.find(GROUP_ID, "some-arty-0"), is(arty));
    }

    private ImmutableReleasableModule.Builder createModule() {
        final ImmutableReleasableModule.Builder builder = ImmutableReleasableModule.builder();
        final MavenProject project = new MavenProject();
        project.setGroupId(GROUP_ID);
        project.setArtifactId("some-arty-" + number++);
        builder.project(project);
        builder.isToBeReleased(false);
        builder.relativePathToModule("..");
        final ImmutableModuleVersion.Builder moduleBuilder = ImmutableModuleVersion.builder();
        moduleBuilder.version(TestUtils.fixVersion(1, 0));
        moduleBuilder.releaseTag(ReleaseDateSingleton.getInstance().tagName());
        moduleBuilder.releaseDate(ReleaseDateSingleton.getInstance().releaseDate());
        moduleBuilder.artifact(ImmutableQualifiedArtifact.builder().groupId(GROUP_ID).artifactId(ARTIFACT_ID).build());
        builder.immutableModule(moduleBuilder.build());
        return builder;
    }

    @Test(expected = UnresolvedSnapshotDependencyException.class)
    public void throwExceptionIfNotFound() throws Exception {
        Reactor reactor = new Reactor(asList(createModule().build(), createModule().build()));
        reactor.find(GROUP_ID, "no-such-artifact");
    }

    @Test
    public void ifNotFoundThenAUnresolvedSnapshotDependencyExceptionIsThrown() throws Exception {
        Reactor reactor = new Reactor(asList(createModule().build(), createModule().build()));
        try {
            reactor.find(GROUP_ID, ARTIFACT_ID);
            Assert.fail("Should have thrown");
        } catch (UnresolvedSnapshotDependencyException e) {
            assertThat(e.getMessage(), equalTo("Could not find my.great.group:some-arty"));
        }
    }
}
