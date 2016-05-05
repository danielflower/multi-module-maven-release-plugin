package com.github.danielflower.mavenplugins.release;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.pom.Updater;
import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.reactor.ReactorBuilderFactory;
import com.github.danielflower.mavenplugins.release.scm.AnnotatedTag;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

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
public class ReleaseMojo extends BaseMojo {

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

	private final Updater pomUpdater;

	@Inject
	public ReleaseMojo(final ReactorBuilderFactory builderFactory, final SCMRepository repository,
			final Updater pomUpdater) throws ValidationException {
		super(builderFactory, repository);
		this.pomUpdater = pomUpdater;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final Log log = getLog();

		try {
			configureJsch(log);
			repository.errorIfNotClean();

			final Reactor reactor = newReactor();

			final List<AnnotatedTag> proposedTags = figureOutTagNamesAndThrowIfAlreadyExists(reactor);

			final List<File> changedFiles = pomUpdater.updatePoms(getLog(), reactor);

			// Do this before running the maven build in case the build uploads
			// some artifacts and then fails. If it is
			// not tagged in a half-failed build, then subsequent releases will
			// re-use a version that is already in Nexus
			// and so fail. The downside is that failed builds result in tags
			// being pushed.
			tagAndPushRepo(log, proposedTags);

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
				revertChanges(changedFiles, true); // throw if you
													// can't revert
													// as that is
													// the root
													// problem
			} finally {
				revertChanges(changedFiles, false); // warn if you
													// can't revert
													// but keep
													// throwing the
													// original
													// exception so
													// the root
													// cause isn't
													// lost
			}

		} catch (final IOException e) {
			printBigErrorMessageAndThrow(e.getMessage(), Collections.<String> emptyList());
		} catch (final ValidationException e) {
			printBigErrorMessageAndThrow(e.getMessage(), e.getMessages());
		} catch (final GitAPIException gae) {

			final StringWriter sw = new StringWriter();
			gae.printStackTrace(new PrintWriter(sw));
			final String exceptionAsString = sw.toString();

			printBigErrorMessageAndThrow("Could not release due to a Git error",
					asList("There was an error while accessing the Git repository. The error returned from git was:",
							gae.getMessage(), "Stack trace:", exceptionAsString));
		}
	}

	private void tagAndPushRepo(final Log log, final List<AnnotatedTag> proposedTags)
			throws ValidationException, GitAPIException {
		for (final AnnotatedTag proposedTag : proposedTags) {
			log.info("About to tag the repository with " + proposedTag.name());
			repository.tagRepoAndPush(proposedTag);
		}
	}

	private void revertChanges(final List<File> changedFiles, final boolean throwIfError)
			throws IOException, ValidationException, MojoExecutionException {
		if (!repository.revertChanges(getLog(), changedFiles)) {
			final String message = "Could not revert changes - working directory is no longer clean. Please revert changes manually";
			if (throwIfError) {
				throw new MojoExecutionException(message);
			} else {
				getLog().warn(message);
			}
		}
	}
}
