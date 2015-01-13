package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.MvnRunner.mvn;

public class HelpTest {

    public static final String multi_module_release_help = "multi-module-release:help";

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void runningTheHelpMojoTellsYouAboutThePluging() throws IOException {
        assertThat(
                mvn(multi_module_release_help),
                containsStrings(
                        "This plugin has 2 goals:",
                        "multi-module-release:release",
                        multi_module_release_help));
    }

    @Test
    public void canShowInformationAboutTheReleaseGoal() throws IOException {
        assertThat(
                mvn(multi_module_release_help, "-Dgoal=release", "-Ddetail=true"),
                containsStrings(
                        "The goals to run against the project during a release",
                        "The release part of the version number to release"));
    }

    private static Matcher<Iterable<? super String>> containsStrings(String... strings) {
        List<Matcher<Iterable<? super String>>> matchers = new ArrayList<Matcher<Iterable<? super String>>>();
        for (String s : strings)
            matchers.add(CoreMatchers.hasItem(containsString(s)));

        return allOf((Iterable) matchers);
    }
}
