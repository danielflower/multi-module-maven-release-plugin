package com.github.danielflower.mavenplugins.release;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.danielflower.mavenplugins.release.pom.ChangeSet;
import com.github.danielflower.mavenplugins.release.pom.Updater;
import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.scm.ProposedTags;

/**
 * Releases the project.
 */
@Mojo(name = "release", requiresDirectInvocation = true, // this should not be
															// bound to a phase
															// as this plugin
															// starts a phase
															// itself
inheritByDefault = true, // so you can configure this in a shared parent pom
requiresProject = true, // this can only run against a maven project
aggregator = true // the plugin should only run once against the aggregator pom
)
public class ReleaseMojo extends NextMojo {

	/**
	 * <p>
	 * The goals to run against the project during a release. By default this is
	 * "deploy" which means the release version of your artifact will be tested
	 * and deployed.
	 * </p>
	 * <p>
	 * You can specify more goals and maven options. For example if you want to
	 * perform a clean, build a maven site, and then deploys it, use:
	 * </p>
	 * 
	 * <pre>
	 * {@code
	 * <releaseGoals>
	 *     <releaseGoal>clean</releaseGoal>
	 *     <releaseGoal>site</releaseGoal>
	 *     <releaseGoal>deploy</releaseGoal>
	 * </releaseGoals>
	 * }
	 * </pre>
	 */
	@Parameter(alias = "releaseGoals")
	private List<String> goals;

	/**
	 * <p>
	 * Profiles to activate during the release.
	 * </p>
	 * <p>
	 * Note that if any profiles are activated during the build using the `-P`
	 * or `--activate-profiles` will also be activated during release. This
	 * gives two options for running releases: either configure it in the plugin
	 * configuration, or activate profiles from the command line.
	 * </p>
	 * 
	 * @since 1.0.1
	 */
	@Parameter(alias = "releaseProfiles")
	private List<String> releaseProfiles;

	/**
	 * If true then tests will not be run during a release. This is the same as
	 * adding -DskipTests=true to the release goals.
	 */
	@Parameter(alias = "skipTests", defaultValue = "false", property = "skipTests")
	private boolean skipTests;

	/**
	 * Specifies a custom, user specific Maven settings file to be used during
	 * the release build.
	 */
	@Parameter(property = "userSettings")
	private File userSettings;

	/**
	 * Specifies a custom, global Maven settings file to be used during the
	 * release build.
	 */
	@Parameter(property = "globalSettings")
	private File globalSettings;

	/**
	 * Specifies a custom directory which should be used as local Maven
	 * repository.
	 */
	@Parameter(property = "localMavenRepo")
	private File localMavenRepo;

	@Component
	private Updater pomUpdater;

	void setUpdater(final Updater pomUpdater) {
		this.pomUpdater = pomUpdater;
	}

	@Override
	protected void execute(final Reactor reactor, final ProposedTags proposedTags)
			throws MojoExecutionException, PluginException {
		try (final ChangeSet changedFiles = pomUpdater.updatePoms(reactor)) {

			// Do this before running the maven build in case the build uploads
			// some artifacts and then fails. If it is
			// not tagged in a half-failed build, then subsequent releases will
			// re-use a version that is already in Nexus
			// and so fail. The downside is that failed builds result in tags
			// being pushed.
			proposedTags.tagAndPushRepo();

			try {
				final ReleaseInvoker invoker = new ReleaseInvoker(getLog(), project);
				invoker.setGlobalSettings(globalSettings);
				invoker.setUserSettings(userSettings);
				invoker.setLocalMavenRepo(localMavenRepo);
				invoker.setGoals(goals);
				invoker.setModulesToRelease(modulesToRelease);
				invoker.setReleaseProfiles(releaseProfiles);
				invoker.setSkipTests(skipTests);
				invoker.setDebugEnabled(debugEnabled);
				invoker.setStacktraceEnabled(stacktraceEnabled);
				invoker.runMavenBuild(reactor);
			} catch (final Exception e) {
				changedFiles.setFailure("Exception occurred while release invokation!", e);
			}
		}
	}
}
