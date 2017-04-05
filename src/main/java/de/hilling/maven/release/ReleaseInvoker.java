package de.hilling.maven.release;

import static java.lang.String.format;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
 * @author Roland Hauser sourcepond@gmail.com
 */
class ReleaseInvoker {
    static final String DEPLOY     = "deploy";
    static final String SKIP_TESTS = "-DskipTests=true";
    private final Log               log;
    private final MavenProject      project;
    private final InvocationRequest request;
    private final Invoker           invoker;
    private       boolean           skipTests;
    private       List<String>      goals;
    private       List<String>      modulesToRelease;
    private       List<String>      releaseProfiles;

    {
        goals = new ArrayList<>();
        goals.add(DEPLOY);
    }

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

    final void setGoals(final List<String> goalsOrNull) {
        goals = goalsOrNull;
    }

    private List<String> getModulesToRelease() {
        return modulesToRelease == null
               ? Collections.emptyList()
               : modulesToRelease;
    }

    final void setModulesToRelease(final List<String> modulesToReleaseOrNull) {
        modulesToRelease = modulesToReleaseOrNull;
    }

    private List<String> getReleaseProfilesOrNull() {
        return releaseProfiles;
    }

    final void setReleaseProfiles(final List<String> releaseProfilesOrNull) {
        releaseProfiles = releaseProfilesOrNull;
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

    public final void runMavenBuild(final Reactor reactor) throws MojoExecutionException {
        request.setInteractive(false);
        request.setShowErrors(true);
        request.setDebug(log.isDebugEnabled());

        if (skipTests) {
            goals.add(SKIP_TESTS);
        }

        request.setGoals(goals);

        final List<String> profiles = profilesToActivate();
        request.setProfiles(profiles);

        request.setAlsoMake(true);
        final List<String> changedModules = reactor.getModulesInBuildOrder().stream()
                                                   .filter(ReleasableModule::isToBeReleased)
                                                   .map(ReleasableModule::getRelativePathToModule)
                                                   .collect(Collectors.toList());
        request.setProjects(changedModules);

        final String profilesInfo = profiles.isEmpty()
                                    ? "no profiles activated"
                                    : "profiles " + profiles;

        log.info("building projects " + changedModules.stream().collect(Collectors.joining(",")));
        log.info(format("About to run mvn %s with %s", this.goals, profilesInfo));

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
        final List<String> profiles = new ArrayList<>();
        if (getReleaseProfilesOrNull() != null) {
            profiles.addAll(getReleaseProfilesOrNull());
        }
        for (final Object activatedProfile : project.getActiveProfiles()) {
            profiles.add(((org.apache.maven.model.Profile) activatedProfile).getId());
        }
        return profiles;
    }
}
