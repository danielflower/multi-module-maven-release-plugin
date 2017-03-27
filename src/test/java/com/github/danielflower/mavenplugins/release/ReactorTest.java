package com.github.danielflower.mavenplugins.release;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;

public class ReactorTest {

    private ReleaseInfo previousRelease;
    private int number;

    @Before
    public void setUp() {
        number = 0;
        previousRelease = TestUtils.releaseInfo(2, 4, "testtag", "some-arty");
    }

    @Test
    public void canFindModulesByGroupAndArtifactName() throws Exception {
        ReleasableModule arty = aModule().groupId("my.great.group").artifactId("some-arty").build();
        Reactor reactor = new Reactor(asList(
            aModule().build(), arty, aModule().build()
        ), previousRelease);
        assertThat(reactor.find("my.great.group", "some-arty", "1.0-SNAPSHOT"), is(arty));
        assertThat(reactor.findByLabel("my.great.group:some-arty"), is(arty));
    }

    private ImmutableReleasableModule.Builder aModule() {
        final ImmutableReleasableModule.Builder builder = ImmutableReleasableModule.builder();
        builder.groupId("my.great.group").artifactId("some-arty" + number++);
        return builder;
    }

    @Test
    public void findOrReturnNullReturnsNullIfNotFound() throws Exception {
        Reactor reactor = new Reactor(asList(
            aModule().build(), aModule().build()
        ), previousRelease);
        assertThat(reactor.findByLabel("my.great.group:some-arty"), is(nullValue()));
    }

    @Test
    public void ifNotFoundThenAUnresolvedSnapshotDependencyExceptionIsThrown() throws Exception {
        Reactor reactor = new Reactor(asList(
            aModule().build(), aModule().build()
        ), previousRelease);
        try {
            reactor.find("my.great.group", "some-arty", "1.0-SNAPSHOT");
            Assert.fail("Should have thrown");
        } catch (UnresolvedSnapshotDependencyException e) {
            assertThat(e.getMessage(), equalTo("Could not find my.great.group:some-arty:1.0-SNAPSHOT"));
        }
    }

    private static class NeverChanged implements DiffDetector {
        @Override
        public boolean hasChangedSince(String modulePath, List<String> childModules, Collection<AnnotatedTag> tags) throws IOException {
            return false;
        }
    }
}
