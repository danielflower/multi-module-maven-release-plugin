package com.github.danielflower.mavenplugins.release;

import org.junit.Assert;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ReleasableModuleBuilder.aModule;

public class ReactorTest {

    @Test
    public void canFindModulesByGroupAndArtifactName() throws Exception {
        ReleasableModule arty = aModule().withGroupId("my.great.group").withArtifactId("some-arty").build();
        Reactor reactor = new Reactor(asList(
            aModule().build(), arty, aModule().build()
        ));
        assertThat(reactor.find("anything", "my.great.group", "some-arty"), is(arty));
    }

    @Test
    public void ifNotFoundThenAValidationExceptionIsThrown() throws Exception {
        Reactor reactor = new Reactor(asList(
            aModule().build(), aModule().build()
        ));
        try {
            reactor.find("the parent reference in my-thing/pom.xml", "my.great.group", "some-arty");
            Assert.fail("Should have thrown");
        } catch (ValidationException e) {
            assertThat(e.getMessages(), oneOf(containsString("The artifact my.great.group:some-arty referenced from the parent reference in my-thing/pom.xml is a SNAPSHOT in your project however it was not found")));
        }
    }
}
