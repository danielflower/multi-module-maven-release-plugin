package com.github.danielflower.mavenplugins.release;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;

/**
 * @author Roland Hauser sourcepond@gmail.com
 *
 */
class ReleaseInvoker {
	static final String DEPLOY = "deploy";
	static final String SKIP_TESTS = "-DskipTests=true";
	static final String LOCAL_REPO = "-Dmaven.repo.local=%s";
	private final Log log;
	private final MavenProject project;
	private final InvocationRequest request;
	private final Invoker invoker;
	private boolean skipTests;
	private boolean debugEnabled;
	private boolean stacktraceEnabled;
	private File localMavenRepo;
	private List<String> goals;
	private List<String> modulesToRelease;
	private List<String> releaseProfiles;

	public ReleaseInvoker(final Log log, final MavenProject project) {
		this(log, project, new DefaultInvocationRequest(), new DefaultInvoker());
	}

	public ReleaseInvoker(final Log log, final MavenProject project, final InvocationRequest request,
			final Invoker invoker) {
		this.log = log;
		this.project = project;
		this.request = request;
		this.invoker = invoker;
	}

	private List<String> getGoals() {
		if (goals == null || goals.isEmpty()) {
			goals = new ArrayList<String>();
			goals.add(DEPLOY);
		}
		return goals;
	}

	private List<String> getModulesToRelease() {
		return modulesToRelease == null ? Collections.<String> emptyList() : modulesToRelease;
	}

	private List<String> getReleaseProfilesOrNull() {
		return releaseProfiles;
	}

	final void setGoals(final List<String> goalsOrNull) {
		goals = goalsOrNull;
	}

	final void setModulesToRelease(final List<String> modulesToReleaseOrNull) {
		modulesToRelease = modulesToReleaseOrNull;
	}

	final void setReleaseProfiles(final List<String> releaseProfilesOrNull) {
		releaseProfiles = releaseProfilesOrNull;
	}

	final void setDebugEnabled(final boolean debugEnabled) {
		this.debugEnabled = debugEnabled;
	}

	final void setStacktraceEnabled(final boolean stacktraceEnabled) {
		this.stacktraceEnabled = stacktraceEnabled;
	}

	final void setSkipTests(final boolean skipTests) {
		this.skipTests = skipTests;
	}

	final void setGlobalSettings(final File globalSettings) {
		request.setGlobalSettingsFile(globalSettings);
	}

	final void setUserSettings(final File userSettings) {
		request.setUserSettingsFile(userSettings);
	}

	final void setLocalMavenRepo(final File localMavenRepo) {
		this.localMavenRepo = localMavenRepo;
	}

	public final void runMavenBuild(final Reactor reactor) throws MojoExecutionException, IOException {
		request.setInteractive(false);
		request.setShowErrors(true);
		request.setDebug(log.isDebugEnabled());

		final List<String> goals = getGoals();
		if (skipTests) {
			goals.add(SKIP_TESTS);
		}
		if (localMavenRepo != null) {
			goals.add(format(LOCAL_REPO, localMavenRepo.getCanonicalFile()));
		}
		if (debugEnabled) {
			goals.add("-X");
		}
		if (stacktraceEnabled) {
			goals.add("-e");
		}
		request.setGoals(getGoals());

		final List<String> profiles = profilesToActivate();
		request.setProfiles(profiles);

		request.setAlsoMake(true);
		final List<String> changedModules = new ArrayList<String>();
		final List<String> modulesToRelease = getModulesToRelease();
		for (final ReleasableModule releasableModule : reactor) {
			final String modulePath = releasableModule.getRelativePathToModule();
			final boolean userExplicitlyWantsThisToBeReleased = modulesToRelease.contains(modulePath);
			final boolean userImplicitlyWantsThisToBeReleased = modulesToRelease.isEmpty();
			if (userExplicitlyWantsThisToBeReleased
					|| (userImplicitlyWantsThisToBeReleased && releasableModule.willBeReleased())) {
				changedModules.add(modulePath);
			}
		}
		request.setProjects(changedModules);

		final String profilesInfo = profiles.isEmpty() ? "no profiles activated" : "profiles " + profiles;

		log.info(format("About to run mvn %s with %s", goals, profilesInfo));

		try {
			final InvocationResult result = invoker.execute(request);
			if (result.getExitCode() != 0) {
				throw new MojoExecutionException("Maven execution returned code " + result.getExitCode());
			}
		} catch (final MavenInvocationException e) {
			throw new MojoExecutionException("Failed to build artifact", e);
		}
	}

	private List<String> profilesToActivate() {
		final List<String> profiles = new ArrayList<String>();
		if (getReleaseProfilesOrNull() != null) {
			for (final String releaseProfile : getReleaseProfilesOrNull()) {
				profiles.add(releaseProfile);
			}
		}
		for (final Object activatedProfile : project.getActiveProfiles()) {
			profiles.add(((org.apache.maven.model.Profile) activatedProfile).getId());
		}
		return profiles;
	}
}
