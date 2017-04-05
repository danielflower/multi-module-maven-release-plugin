package com.github.danielflower.mavenplugins.release;

import java.io.File;
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

import com.github.danielflower.mavenplugins.release.releaseinfo.ReleaseInfoStorage;
import com.github.danielflower.mavenplugins.release.repository.LocalGitRepo;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableReleaseInfo;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseDateSingleton;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;

/**
 * Releases the project.
 */
@Mojo(name = "release", requiresDirectInvocation = true,
      // this should not be bound to a phase as this plugin starts a phase itself
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
     * Profiles to activate during the release.
     * </p>
     * <p>
     * Note that if any profiles are activated during the build using the `-P` or `--activate-profiles` will also be activated during release.
     * This gives two options for running releases: either configure it in the plugin configuration, or activate profiles from the command line.
     * </p>
     *
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
    @Parameter(alias = "pushTags", defaultValue = "true", property = "push")
    private boolean pushTags;

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

    private static List<File> updatePomsAndReturnChangedFiles(Log log, LocalGitRepo repo, Reactor reactor) throws
                                                                                                           MojoExecutionException,
                                                                                                           ValidationException {
        PomUpdater pomUpdater = new PomUpdater(log, reactor);
        PomUpdater.UpdateResult result = pomUpdater.updateVersion();
        if (!result.success()) {
            log.info("Going to revert changes because there was an error.");
            repo.revertChanges(log, result.alteredPoms);
            if (result.unexpectedException != null) {
                throw new ValidationException("Unexpected exception while setting the release versions in the pom",
                                              result.unexpectedException);
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

    @Override
    public void executeConcreteMojo() throws MojoExecutionException, MojoFailureException, GitAPIException {

        configureJsch();

        LocalGitRepo repo = LocalGitRepo.fromCurrentDir(
            getRemoteUrlOrNullIfNoneSet(project.getOriginalModel().getScm(), project.getModel().getScm()));
        repo.errorIfNotClean();

        final ReleaseInfoStorage infoStorage = new ReleaseInfoStorage(project.getBasedir(), repo.git);
        ReleaseInfo previousRelease = infoStorage.load();
        getLog().info("previous release: " + previousRelease);

        Reactor reactor = Reactor
                              .fromProjects(getLog(), repo, project, projects, modulesToForceRelease, noChangesAction,
                                            bugfixRelease, previousRelease);
        if (reactor == null) {
            return;
        }

        final List<ReleasableModule> releasableModules = reactor.getModulesInBuildOrder();

        final ImmutableReleaseInfo.Builder releaseBuilder = ImmutableReleaseInfo.builder().tagName(
            ReleaseDateSingleton.getInstance().tagName());

        List<String> modulesToRelease = new ArrayList<>();
        for (ReleasableModule releasableModule : releasableModules) {
            releaseBuilder.addModules(releasableModule.getImmutableModule());
            if (releasableModule.isToBeReleased()) {
                modulesToRelease.add(releasableModule.getRelativePathToModule());
            }
        }

        final ImmutableReleaseInfo currentRelease = releaseBuilder.build();
        getLog().info("current release: " + currentRelease);
        infoStorage.store(currentRelease);

        List<File> changedFiles = updatePomsAndReturnChangedFiles(getLog(), repo, reactor);

        // Do this before running the maven build in case the build uploads some artifacts and then fails. If it is
        // not tagged in a half-failed build, then subsequent releases will re-use a version that is already in Nexus
        // and so fail. The downside is that failed builds result in tags being pushed.
        tagAndPushRepo(repo, currentRelease);

        try {
            final ReleaseInvoker invoker = new ReleaseInvoker(getLog(), project);
            invoker.setGoals(goals);
            invoker.setModulesToRelease(modulesToRelease);
            invoker.setReleaseProfiles(releaseProfiles);
            invoker.setSkipTests(skipTests);
            invoker.runMavenBuild(reactor);
            revertChanges(repo, changedFiles, true); // throw if you can't revert as that is the root problem
        } finally {
            revertChanges(repo, changedFiles,
                          false); // warn if you can't revert but keep throwing the original exception so the root cause isn't lost
        }
    }

    private void tagAndPushRepo(LocalGitRepo repo, ImmutableReleaseInfo releaseInfo) throws GitAPIException {
        final AnnotatedTag tag = new AnnotatedTag(null, releaseInfo.getTagName().get(), releaseInfo);

        getLog().info("About to tag repository with " + releaseInfo.toString());
        final Ref ref = repo.tagRepo(tag);
        if (pushTags) {
            getLog().info("About to push tags and release-info " + releaseInfo.toString());
            repo.pushAll(ref);
        }
    }

    private void revertChanges(LocalGitRepo repo, List<File> changedFiles, boolean throwIfError) throws
                                                                                                 MojoExecutionException {
        if (!repo.revertChanges(getLog(), changedFiles)) {
            String message = "Could not revert changes - working directory is no longer clean. Please revert changes manually";
            if (throwIfError) {
                throw new MojoExecutionException(message);
            } else {
                getLog().warn(message);
            }
        }
    }
}
