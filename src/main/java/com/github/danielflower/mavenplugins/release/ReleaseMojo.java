package com.github.danielflower.mavenplugins.release;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

import com.github.danielflower.mavenplugins.release.releaseinfo.ReleaseInfoStorage;
import com.github.danielflower.mavenplugins.release.repository.LocalGitRepo;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableModuleVersion;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableReleaseInfo;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;

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
     * Push tags to remote repository as they are created.
     */
    @Parameter(alias = "pushTags", defaultValue="true", property="push")
    private boolean pushTags;



    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();

        try {
            configureJsch(log);

            LocalGitRepo repo = LocalGitRepo.fromCurrentDir(getRemoteUrlOrNullIfNoneSet(project.getOriginalModel().getScm(), project.getModel().getScm()));
            repo.errorIfNotClean();

            final ReleaseInfoStorage infoStorage = new ReleaseInfoStorage(project.getBasedir(), repo.git);
            ReleaseInfo previousRelease = infoStorage.load();

            Reactor reactor = Reactor.fromProjects(log, repo, project, projects, modulesToForceRelease,
                                                   noChangesAction, bugfixRelease, previousRelease);
            if (reactor == null) {
                return;
            }

            List<ImmutableModuleVersion> newVersions = figureOutTagNamesAndThrowIfAlreadyExists(reactor.getModulesInBuildOrder(),
                                                                                        repo);

            List<File> changedFiles = updatePomsAndReturnChangedFiles(log, repo, reactor);

            final ImmutableReleaseInfo.Builder currentRelease = ImmutableReleaseInfo.builder();
            currentRelease.tagName(tagName());

            for (ImmutableModuleVersion oldVersion : previousRelease.getModules()) {
                if(newVersions.stream().noneMatch(version -> oldVersion.getArtifact().equals(version.getArtifact()))) {
                    log.info("not being released: " + oldVersion.getArtifact());
                    currentRelease.addModules(oldVersion);
                }
            }
            for (ImmutableModuleVersion newVersion : newVersions) {
                currentRelease.addModules(newVersion);
            }
            final RevCommit releaseCommit = infoStorage.store(currentRelease.build());

            // Do this before running the maven build in case the build uploads some artifacts and then fails. If it is
            // not tagged in a half-failed build, then subsequent releases will re-use a version that is already in Nexus
            // and so fail. The downside is that failed builds result in tags being pushed.
            tagAndPushRepo(log, repo, newVersions, releaseCommit);

            try {
            	final ReleaseInvoker invoker = new ReleaseInvoker(getLog(), project);
            	invoker.setGoals(goals);
            	invoker.setModulesToRelease(modulesToRelease);
            	invoker.setReleaseProfiles(releaseProfiles);
            	invoker.setSkipTests(skipTests);
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
        }
    }

    private void tagAndPushRepo(Log log, LocalGitRepo repo, List<ImmutableModuleVersion> versions,
                                RevCommit releaseCommit) throws GitAPIException {
        final ImmutableReleaseInfo.Builder builder = ImmutableReleaseInfo.builder();
        builder.tagName(tagName());
        builder.addAllModules(versions);
        final ImmutableReleaseInfo releaseInfo = builder.build();
        final AnnotatedTag tag = new AnnotatedTag(null, releaseInfo.getTagName().get(), releaseInfo);

        log.info("About to tag repository with " + releaseInfo.toString());
        final Ref ref = repo.tagRepo(tag);
        if (pushTags) {
            log.info("About to push tags and release-info " + releaseInfo.toString());
            repo.pushAll(ref);
        }
    }

    static String getRemoteUrlOrNullIfNoneSet(Scm originalScm, Scm actualScm) throws ValidationException {
        if (originalScm == null) {
            // No scm was specified, so don't inherit from any parent poms as they are probably used in different git repos
            return null;
        }

        // There is an SCM specified, so the actual SCM with derived values is used in case (so that variables etc are interpolated)
        String remote = actualScm.getDeveloperConnection();
        if (remote == null) {
            remote = actualScm.getConnection();
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
                List<String> messages = new ArrayList<>();
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
}
