package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;
import scaffolding.MvnRunner;
import scaffolding.TestProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class HelpTest {

    public static final String multi_module_release_help = "multi-module-release:help";
    private static TestProject someProject;

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException, IOException, GitAPIException {
        MvnRunner.installReleasePluginToLocalRepo();
        someProject = TestProject.singleModuleProject();
    }

    @Test
    public void runningTheHelpMojoTellsYouAboutThePlugin() throws IOException {
        assertThat(
                mvn(multi_module_release_help),
                containsStrings(
                        "This plugin has 2 goals:",
                        "multi-module-release:release",
                        multi_module_release_help));
    }

    private List<String> mvn(String... commands) throws IOException {
        return MvnRunner.runMaven(someProject.localDir, commands);
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
            matchers.add(CoreMatchers.<String>hasItem(containsString(s)));

        return CoreMatchers.allOf((Iterable) matchers);
    }
}
