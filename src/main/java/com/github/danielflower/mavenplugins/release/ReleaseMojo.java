package com.github.danielflower.mavenplugins.release;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import org.eclipse.jgit.transport.JschConfigSessionFactory;

/**
 * Releases the project.
 */
@Mojo(
    name = "release",
    requiresDirectInvocation = true, // this should not be bound to a phase as this plugin starts a phase itself
    inheritByDefault = true, // so you can configure this in a shared parent pom
    requiresProject = true, // this can only run against a maven project
    aggregator = true // the plugin should only run once against the aggregator pom
)
public class ReleaseMojo extends AbstractMojo {

    /**
     * The Maven Project.
     */
    @Parameter(property = "project", required = true, readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "projects", required = true, readonly = true, defaultValue = "${reactorProjects}")
    private List<MavenProject> projects;

    /**
     * <p>
     * The build number to use in the release version. Given a snapshot version of "1.0-SNAPSHOT"
     * and a buildNumber value of "2", the actual released version will be "1.0.2".
     * </p>
     * <p>
     * By default, the plugin will automatically find a suitable build number. It will start at version
     * 0 and increment this with each release.
     * </p>
     * <p>
     * This can be specified using a command line parameter ("-DbuildNumber=2") or in this plugin's configuration.
     * </p>
     */
    @Parameter(property = "buildNumber")
    private Long buildNumber;

    /**
     * <p>
     * The goals to run against the project during a release. By default this is "deploy" which
     * means the release version of your artifact will be tested and deployed.
     * </p>
     * <p>
     * You can specify more goals and maven options. For example if you want to perform
     * a clean, build a maven site, and then deploys it, use:
     * </p>
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
     *     Profiles to activate during the release.
     * </p>
     * <p>
     *     Note that if any profiles are activated during the build using the `-P` or `--activate-profiles` will also be activated during release.
     *     This gives two options for running releases: either configure it in the plugin configuration, or activate profiles from the command line.
     * </p>
     * @since 1.0.1
     */
    @Parameter(alias = "releaseProfiles")
    private List<String> releaseProfiles;

    /**
     * If true then tests will not be run during a release.
     * This is the same as adding -DskipTests=true to the release goals.
     */
    @Parameter(alias = "skipTests", defaultValue = "false", property = "skipTests")
    private boolean skipTests;

    /**
     * The modules to release, or no  value to to release the project from the root pom, which is the default.
     * The selected module plus any other modules it needs will be built and released also.
     * When run from the command line, this can be a comma-separated list of module names.
     */
    @Parameter(alias = "modulesToRelease", property = "modulesToRelease")
    private List<String> modulesToRelease;

    /**
     * A module to force release on, even if no changes has been detected.
     */
    @Parameter(alias = "forceRelease", property = "forceRelease")
    private List<String> modulesToForceRelease;

    @Parameter(property = "disableSshAgent")
    private boolean disableSshAgent;

    /**
     * If true then the SNAPSHOT parent check is disabled, allowing any of the modules selected for release to have any SNAPSHOT parents.
     * SNAPSHOT parents that are part of the reactor for this release build will still be transformed, but no other SNAPSHOT parents will be transformed.
     */
    @Parameter(alias = "allowSnapshotParents", defaultValue = "false", property = "allowSnapshotParents")
    private boolean allowSnapshotParents;

    /**
     * A list of parents that are permitted to have SNAPSHOT versions.
     * SNAPSHOT parents that are part of the reactor for this release build will still be transformed, but no other SNAPSHOT parents will be transformed.
     * Placing an artifact in this list will have no effect if the artifact is also in the reactor.
     * If allowSnapshotParents is true, this list is ignored.
     */
    @Parameter(alias = "allowedSnapshotParents")
    private List<ExemptSnapshotArtifact> allowedSnapshotParents;

    /**
     * If true then the SNAPSHOT dependency check is disabled, allowing any of the modules selected for release to have any SNAPSHOT dependencies.
     * SNAPSHOT dependencies that are part of the reactor for this release build will still be transformed, but no other SNAPSHOT dependencies will be transformed.
     */
    @Parameter(alias = "allowSnapshotDependencies", defaultValue = "false", property = "allowSnapshotDependencies")
    private boolean allowSnapshotDependencies;

    /**
     * A list of dependencies that are permitted to have SNAPSHOT versions.
     * SNAPSHOT dependencies that are part of the reactor for this release build will still be transformed, but no other SNAPSHOT dependencies will be transformed.
     * Placing an artifact in this list will have no effect if the artifact is also in the reactor.
     * If allowSnapshotDependencies is true, this list is ignored.
     */
    @Parameter(alias = "allowedSnapshotDependencies")
    private List<ExemptSnapshotArtifact> allowedSnapshotDependencies;

    /**
     * If true then the SNAPSHOT plugin check is disabled, allowing any of the modules selected for release to have any SNAPSHOT plugins.
     */
    @Parameter(alias = "allowSnapshotPlugins", defaultValue = "false", property = "allowSnapshotPlugins")
    private boolean allowSnapshotPlugins;

    /**
     * A list of plugins that are permitted to have SNAPSHOT versions.
     * If allowSnapshotPlugins is true, this list is ignored.
     */
    @Parameter(alias = "allowedSnapshotPlugins")
    private List<ExemptSnapshotArtifact> allowedSnapshotPlugins;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();

        try {
            configureJsch(log);

            LocalGitRepo repo = LocalGitRepo.fromCurrentDir(getRemoteUrlOrNullIfNoneSet(project.getScm()));
            repo.errorIfNotClean();

            Reactor reactor = Reactor.fromProjects(log, repo, project, projects, buildNumber, modulesToForceRelease);

            List<AnnotatedTag> proposedTags = figureOutTagNamesAndThrowIfAlreadyExists(reactor.getModulesInBuildOrder(), repo, modulesToRelease);

            SnapshotStrategy snapshotStrategy = new SnapshotStrategy(allowSnapshotParents, allowedSnapshotParents, allowSnapshotDependencies, allowedSnapshotDependencies, allowSnapshotPlugins, allowedSnapshotPlugins);
            List<File> changedFiles = updatePomsAndReturnChangedFiles(log, repo, reactor, snapshotStrategy);

            // Do this before running the maven build in case the build uploads some artifacts and then fails. If it is
            // not tagged in a half-failed build, then subsequent releases will re-use a version that is already in Nexus
            // and so fail. The downside is that failed builds result in tags being pushed.
            tagAndPushRepo(log, repo, proposedTags);

            try {
                runMavenBuild(reactor);
                revertChanges(log, repo, changedFiles, true); // throw if you can't revert as that is the root problem
            } finally {
                revertChanges(log, repo, changedFiles, false); // warn if you can't revert but keep throwing the original exception so the root cause isn't lost
            }


        } catch (ValidationException e) {
            printBigErrorMessageAndThrow(log, e.getMessage(), e.getMessages());
        } catch (GitAPIException gae) {

            StringWriter sw = new StringWriter();
            gae.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            printBigErrorMessageAndThrow(log, "Could not release due to a Git error",
                asList("There was an error while accessing the Git repository. The error returned from git was:",
                    gae.getMessage(), "Stack trace:", exceptionAsString));
        }
    }

    private void configureJsch(Log log) {
        if(!disableSshAgent) {
            JschConfigSessionFactory.setInstance(new SshAgentSessionFactory(log));
        }
    }

    private void tagAndPushRepo(Log log, LocalGitRepo repo, List<AnnotatedTag> proposedTags) throws GitAPIException {
        for (AnnotatedTag proposedTag : proposedTags) {
            log.info("About to tag the repository with " + proposedTag.name());
            repo.tagRepoAndPush(proposedTag);
        }
    }

    private static String getRemoteUrlOrNullIfNoneSet(Scm scm) throws ValidationException {
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

    private static void revertChanges(Log log, LocalGitRepo repo, List<File> changedFiles, boolean throwIfError) throws MojoExecutionException {
        if (!repo.revertChanges(log, changedFiles)) {
            String message = "Could not revert changes - working directory is no longer clean. Please revert changes manually";
            if (throwIfError) {
                throw new MojoExecutionException(message);
            } else {
                log.warn(message);
            }
        }
    }

    private static List<File> updatePomsAndReturnChangedFiles(Log log, LocalGitRepo repo, Reactor reactor, SnapshotStrategy snapshotStrategy) throws MojoExecutionException, ValidationException {
        PomUpdater pomUpdater = new PomUpdater(log, reactor, snapshotStrategy);
        PomUpdater.UpdateResult result = pomUpdater.updateVersion();
        if (!result.success()) {
            log.info("Going to revert changes because there was an error.");
            repo.revertChanges(log, result.alteredPoms);
            if (result.unexpectedException != null) {
                throw new ValidationException("Unexpected exception while setting the release versions in the pom", result.unexpectedException);
            } else {
                String summary = "Cannot release with references to snapshot dependencies";
                List<String> messages = new ArrayList<String>();
                messages.add(summary);
                messages.add("The following dependency errors were found:");
                for (String dependencyError : result.dependencyErrors) {
                    messages.add(" * " + dependencyError);
                }
                throw new ValidationException(summary, messages);
            }
        }
        return result.alteredPoms;
    }

    private static List<AnnotatedTag> figureOutTagNamesAndThrowIfAlreadyExists(List<ReleasableModule> modules, LocalGitRepo git, List<String> modulesToRelease) throws GitAPIException, ValidationException {
        List<AnnotatedTag> tags = new ArrayList<AnnotatedTag>();
        for (ReleasableModule module : modules) {
            if (!module.willBeReleased()) {
                continue;
            }
            if (modulesToRelease == null || modulesToRelease.size() == 0 || module.isOneOf(modulesToRelease)) {
                String tag = module.getTagName();
                if (git.hasLocalTag(tag)) {
                    String summary = "There is already a tag named " + tag + " in this repository.";
                    throw new ValidationException(summary, asList(
                        summary,
                        "It is likely that this version has been released before.",
                        "Please try incrementing the build number and trying again."
                    ));
                }

                AnnotatedTag annotatedTag = AnnotatedTag.create(tag, module.getVersion(), module.getBuildNumber());
                tags.add(annotatedTag);
            }
        }
        List<String> matchingRemoteTags = git.remoteTagsFrom(tags);
        if (matchingRemoteTags.size() > 0) {
            String summary = "Cannot release because there is already a tag with the same build number on the remote Git repo.";
            List<String> messages = new ArrayList<String>();
            messages.add(summary);
            for (String matchingRemoteTag : matchingRemoteTags) {
                messages.add(" * There is already a tag named " + matchingRemoteTag + " in the remote repo.");
            }
            messages.add("Please try releasing again with a new build number.");
            throw new ValidationException(summary, messages);
        }
        return tags;
    }

    private static void printBigErrorMessageAndThrow(Log log, String terseMessage, List<String> linesToLog) throws MojoExecutionException {
        log.error("");
        log.error("");
        log.error("");
        log.error("************************************");
        log.error("Could not execute the release plugin");
        log.error("************************************");
        log.error("");
        log.error("");
        for (String line : linesToLog) {
            log.error(line);
        }
        log.error("");
        log.error("");
        throw new MojoExecutionException(terseMessage);
    }

    private void runMavenBuild(Reactor reactor) throws MojoExecutionException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setInteractive(false);

        if (goals == null) {
            goals = asList("deploy");
        }
        if (skipTests) {
            goals.add("-DskipTests=true");
        }
        request.setShowErrors(true);
        request.setDebug(getLog().isDebugEnabled());
        request.setGoals(goals);
        List<String> profiles = profilesToActivate();
        request.setProfiles(profiles);

        request.setAlsoMake(true);
        List<String> changedModules = new ArrayList<String>();
        for (ReleasableModule releasableModule : reactor.getModulesInBuildOrder()) {
            String modulePath = releasableModule.getRelativePathToModule();
            boolean userExplicitlyWantsThisToBeReleased = modulesToRelease.contains(modulePath);
            boolean userImplicitlyWantsThisToBeReleased = modulesToRelease == null || modulesToRelease.size() == 0;
            if (userExplicitlyWantsThisToBeReleased || (userImplicitlyWantsThisToBeReleased && releasableModule.willBeReleased())) {
                changedModules.add(modulePath);
            }
        }
        request.setProjects(changedModules);

        String profilesInfo = (profiles.size() == 0) ? "no profiles activated" : "profiles " + profiles;

        getLog().info("About to run mvn " + goals + " with " + profilesInfo);

        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Maven execution returned code " + result.getExitCode());
            }
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException("Failed to build artifact", e);
        }
    }

    private List<String> profilesToActivate() {
        List<String> profiles = new ArrayList<String>();
        if (releaseProfiles != null) {
            for (String releaseProfile : releaseProfiles) {
                profiles.add(releaseProfile);
            }
        }
        for (Object activatedProfile : project.getActiveProfiles()) {
            profiles.add(((org.apache.maven.model.Profile) activatedProfile).getId());
        }
        return profiles;
    }

}
