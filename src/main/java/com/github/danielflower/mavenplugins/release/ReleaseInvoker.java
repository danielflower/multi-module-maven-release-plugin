package com.github.danielflower.mavenplugins.release;

import static java.util.Arrays.asList;

import java.io.File;
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

/**
 * @author rolandhauser
 *
 */
class ReleaseInvoker {
	private final Log log;
	private final MavenProject project;
	private final InvocationRequest request;
	private List<String> goals;
	private List<String> modulesToRelease;
	private List<String> releaseProfiles;

	public ReleaseInvoker(final Log log, final MavenProject project) {
		this(log, project, new DefaultInvocationRequest());
	}

	
	public ReleaseInvoker(final Log log, final MavenProject project, final InvocationRequest request) {
		this.log = log;
		this.project = project;
		this.request = request;
	}

	private List<String> getGoals() {
		if (goals == null || goals.isEmpty()) {
			goals = asList("deploy");
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

	final void skipTests(final boolean skipTests) {
		if (skipTests) {
			getGoals().add("-DskipTests=true");
		}
	}

	final void setGlobalSettings(final File globalSettings) {
		request.setGlobalSettingsFile(globalSettings);
	}

	final void setUserSettings(final File userSettings) {
		request.setUserSettingsFile(userSettings);
	}

	protected InvocationRequest createRequest() {
		return new DefaultInvocationRequest();
	}

	public final void runMavenBuild(final Reactor reactor) throws MojoExecutionException {
		final InvocationRequest request = createRequest();
		request.setInteractive(false);

		request.setShowErrors(true);
		request.setDebug(log.isDebugEnabled());
		request.setGoals(getGoals());
		final List<String> profiles = profilesToActivate();
		request.setProfiles(profiles);

		request.setAlsoMake(true);
		final List<String> changedModules = new ArrayList<String>();
		final List<String> modulesToRelease = getModulesToRelease();
		for (final ReleasableModule releasableModule : reactor.getModulesInBuildOrder()) {
			final String modulePath = releasableModule.getRelativePathToModule();
			final boolean userExplicitlyWantsThisToBeReleased = modulesToRelease.contains(modulePath);
			final boolean userImplicitlyWantsThisToBeReleased = modulesToRelease.isEmpty();
			if (userExplicitlyWantsThisToBeReleased
					|| (userImplicitlyWantsThisToBeReleased && releasableModule.willBeReleased())) {
				changedModules.add(modulePath);
			}
		}
		request.setProjects(changedModules);

		final String profilesInfo = (profiles.size() == 0) ? "no profiles activated" : "profiles " + profiles;

		log.info("About to run mvn " + goals + " with " + profilesInfo);

		final Invoker invoker = new DefaultInvoker();
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
