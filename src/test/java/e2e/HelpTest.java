package e2e;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;

import scaffolding.MvnRunner;
import scaffolding.TestProject;

public class HelpTest extends E2ETest {

	private static final String multi_module_release_help = "releaser:help";
	private static TestProject someProject;

	@BeforeClass
	public static void installPluginToLocalRepo() throws MavenInvocationException, IOException, GitAPIException {
		someProject = TestProject.singleModuleProject();
	}

	@Test
	public void runningTheHelpMojoTellsYouAboutThePlugin() throws IOException {
		assertThat(mvn(multi_module_release_help), containsStrings("This plugin has 3 goals:", "releaser:release",
				"releaser:help", multi_module_release_help));
	}

	private List<String> mvn(final String... commands) throws IOException {
		return new MvnRunner().runMaven(someProject.localDir, commands);
	}

	@Test
	public void canShowInformationAboutTheReleaseGoal() throws IOException {
		assertThat(mvn(multi_module_release_help, "-Dgoal=release", "-Ddetail=true"),
				containsStrings("The goals to run against the project during a release",
						"The build number to use in the release version"));
	}

	private static Matcher<Iterable<? super String>> containsStrings(final String... strings) {
		final List<Matcher<Iterable<? super String>>> matchers = new ArrayList<>();
		for (final String s : strings)
			matchers.add(CoreMatchers.hasItem(containsString(s)));

		return allOf((Iterable) matchers);
	}
}
