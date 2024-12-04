package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static java.util.Arrays.asList;

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
public class ReleaseMojo extends BaseMojo {

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
     * Specifies a custom, user specific Maven settings file to be used during the release build.
     *
     * @deprecated In versions prior to 2.1, if the plugin was run with custom user settings the settings were ignored
     * during the release phase. Now that custom settings are inherited, setting this value is no longer needed.
     * Please use the '-s' command line parameter to set custom user settings.
     */
    @Parameter(alias = "userSettings")
    private File userSettings;

    /**
     * Specifies a custom, global Maven settings file to be used during the release build.
     *
     * @deprecated In versions prior to 2.1, if the plugin was run with custom global settings the settings were ignored
     * during the release phase. Now that custom settings are inherited, setting this value is no longer needed.
     * Please use the '-gs' command line parameter to set custom global settings.
     */
    @Parameter(alias = "globalSettings")
    private File globalSettings;

    /**
     * Push tags to remote repository as they are created.
     */
    @Parameter(alias = "pushTags", defaultValue="true", property="push")
    private boolean pushTags;

    /**
     * <p>
     * Reports to generate with updated versions.
     * </p>
     * <p>
     * You can specify list of {@link com.github.danielflower.mavenplugins.release.VersionReport}
     * </p>
     * <pre>{@code
     *  <versionReports>
     *      <versionReport>
     *          <versionsReportFilePath>released-report.txt</versionsReportFilePath>
     *          <versionsReportFormat>FLAT</versionsReportFormat>
     *          <releasedModulesOnly>true</releasedModulesOnly>
     *      </versionReport>
     *      <versionReport>
     *          <versionsReportFilePath>version-report.json</versionsReportFilePath>
     *          <versionsReportFormat>JSON</versionsReportFormat>
     *          <releasedModulesOnly>false</releasedModulesOnly>
     *      </versionReport>
     *  </versionReports>
     * }</pre>
     * @see com.github.danielflower.mavenplugins.release.VersionReport
     */
    @Parameter(alias = "versionReports")
    private List<VersionReport> versionReports;

    /**
     * <p>
     * Specifies paths to ignore when checking for untracked files before releasing.
     * </p>
     * <p>
     * By default, any untracked files in the git repo will prevent the release from starting. For cases when it
     * is not possible to ignore these using a <code>.gitignore</code> file, this setting can be used to ignore
     * specific paths during a release.
     * </p>
     */
    @Parameter(alias = "ignoredUntrackedPaths", property = "ignoredUntrackedPaths")
    private Set<String> ignoredUntrackedPaths = new HashSet<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();

        try {
            configureJsch(log);

            Set<GitOperations> gitOperations = EnumSet.noneOf(GitOperations.class);
            if (pullTags) {
                gitOperations.add(GitOperations.PULL_TAGS);
            }
            if (pushTags) {
                gitOperations.add(GitOperations.PUSH_TAGS);
            }

            LocalGitRepo repo = new LocalGitRepo.Builder()
                .remoteGitOperationsAllowed(gitOperations)
                .remoteGitUrl(getRemoteUrlOrNullIfNoneSet(project.getOriginalModel().getScm(),
                                                          project.getModel().getScm()))
                .credentialsProvider(getCredentialsProvider(log))
                .buildFromCurrentDir();
            repo.errorIfNotClean(ignoredUntrackedPaths);

            ResolverWrapper resolverWrapper = new ResolverWrapper(factory, artifactResolver, remoteRepositories, localRepository);
            Reactor reactor = Reactor.fromProjects(log, repo, project, projects, buildNumber, modulesToForceRelease, noChangesAction, resolverWrapper, versionNamer, ignoredPaths);
            if (reactor == null) {
                return;
            }

            List<AnnotatedTag> proposedTags = figureOutTagNamesAndThrowIfAlreadyExists(reactor.getModulesInBuildOrder(), repo, modulesToRelease);

            List<File> changedFiles = updatePomsAndReturnChangedFiles(log, repo, reactor);

            if (versionReports != null) {
                for (VersionReport versionReport : versionReports) {
                    versionReport.generateVersionReport(log, reactor.getModulesInBuildOrder());
                }
            }

            // Do this before running the maven build in case the build uploads some artifacts and then fails. If it is
            // not tagged in a half-failed build, then subsequent releases will re-use a version that is already in Nexus
            // and so fail. The downside is that failed builds result in tags being pushed.
            tagAndPushRepo(log, repo, proposedTags);

            try {
                final ReleaseInvoker invoker = new ReleaseInvoker(getLog(), project);
                invoker.setGlobalSettings(globalSettings);
                if (userSettings != null) {
                    invoker.setUserSettings(userSettings);
                } else if (getSettings() != null) {
                    File settingsFile = File.createTempFile("tmp", ".xml");
                    settingsFile.deleteOnExit();
                    new DefaultSettingsWriter().write(settingsFile, null, getSettings());
                    invoker.setUserSettings(settingsFile);
                }
                invoker.setGoals(goals);
                invoker.setModulesToRelease(modulesToRelease);
                invoker.setReleaseProfiles(releaseProfiles);
                invoker.setSkipTests(skipTests);
                invoker.setArguments(arguments);

                invoker.runMavenBuild(reactor);
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
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            printBigErrorMessageAndThrow(log, e.getMessage(),
                    asList("There was an error while creating temporary settings file. The error was:", e.getMessage(), "Stack trace:", exceptionAsString));
        }
    }

    private void tagAndPushRepo(Log log, LocalGitRepo repo, List<AnnotatedTag> proposedTags) throws GitAPIException {
        for (AnnotatedTag proposedTag : proposedTags) {
            log.info("About to tag the repository with " + proposedTag.name());
        }

        repo.tagAndPushRepo(proposedTags);
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

    private static List<File> updatePomsAndReturnChangedFiles(Log log, LocalGitRepo repo, Reactor reactor) throws MojoExecutionException, ValidationException {
        PomUpdater pomUpdater = new PomUpdater(log, reactor);
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

    static List<AnnotatedTag> figureOutTagNamesAndThrowIfAlreadyExists(List<ReleasableModule> modules, LocalGitRepo git, List<String> modulesToRelease) throws GitAPIException, ValidationException {
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
        List<String> matchingRemoteTags = git.tagsFrom(tags);
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

}
