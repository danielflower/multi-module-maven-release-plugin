package com.github.danielflower.mavenplugins.release;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.reactor.ReactorBuilderFactory;

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

	@Inject
	public ReleaseMojo(final ReactorBuilderFactory builderFactory) {
		super(builderFactory);
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final Log log = getLog();

		try {
			configureJsch(log);

			final LocalGitRepo repo = LocalGitRepo.fromCurrentDir(getRemoteUrlOrNullIfNoneSet(project.getScm()));
			repo.errorIfNotClean();

			final Reactor reactor = newReactor();

			final List<AnnotatedTag> proposedTags = figureOutTagNamesAndThrowIfAlreadyExists(reactor);

			final List<File> changedFiles = updatePomsAndReturnChangedFiles(reactor);

			// Do this before running the maven build in case the build uploads
			// some artifacts and then fails. If it is
			// not tagged in a half-failed build, then subsequent releases will
			// re-use a version that is already in Nexus
			// and so fail. The downside is that failed builds result in tags
			// being pushed.
			tagAndPushRepo(log, repo, proposedTags);

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
				revertChanges(log, repo, changedFiles, true); // throw if you
																// can't revert
																// as that is
																// the root
																// problem
			} finally {
				revertChanges(log, repo, changedFiles, false); // warn if you
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

	private void tagAndPushRepo(final Log log, final LocalGitRepo repo, final List<AnnotatedTag> proposedTags)
			throws GitAPIException {
		for (final AnnotatedTag proposedTag : proposedTags) {
			log.info("About to tag the repository with " + proposedTag.name());
			repo.tagRepoAndPush(proposedTag);
		}
	}

	private static String getRemoteUrlOrNullIfNoneSet(final Scm scm) throws ValidationException {
		if (scm == null) {
			return null;
		}
		String remote = scm.getDeveloperConnection();
		if (remote == null) {
			remote = scm.getConnection();
		}
		if (remote == null) {
			return null;
		}
		return GitHelper.scmUrlToRemote(remote);
	}

	private static void revertChanges(final Log log, final LocalGitRepo repo, final List<File> changedFiles,
			final boolean throwIfError) throws MojoExecutionException {
		if (!repo.revertChanges(log, changedFiles)) {
			final String message = "Could not revert changes - working directory is no longer clean. Please revert changes manually";
			if (throwIfError) {
				throw new MojoExecutionException(message);
			} else {
				log.warn(message);
			}
		}
	}

	private List<File> updatePomsAndReturnChangedFiles(final Reactor reactor)
			throws MojoExecutionException, ValidationException {
		final PomUpdater pomUpdater = new PomUpdater(getLog(), reactor);
		final PomUpdater.UpdateResult result = pomUpdater.updateVersion();
		if (!result.success()) {
			getLog().info("Going to revert changes because there was an error.");
			reactor.getLocalRepo().revertChanges(getLog(), result.alteredPoms);
			if (result.unexpectedException != null) {
				throw new ValidationException("Unexpected exception while setting the release versions in the pom",
						result.unexpectedException);
			} else {
				final String summary = "Cannot release with references to snapshot dependencies";
				final List<String> messages = new ArrayList<String>();
				messages.add(summary);
				messages.add("The following dependency errors were found:");
				for (final String dependencyError : result.dependencyErrors) {
					messages.add(" * " + dependencyError);
				}
				throw new ValidationException(summary, messages);
			}
		}
		return result.alteredPoms;
	}
}
