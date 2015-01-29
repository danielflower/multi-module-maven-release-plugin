package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
     * and a buildNumber value of "2", the actual released version will be "1.0.2". This can be
     * specified using a command line parameter ("-DbuildNumber=2") or in this plugin's configuration.
     * </p>
     * <p>
     * By default, in no value is specified then this is a value based on the current time
     * such as "20150129135926".
     * </p>
     */
    @Parameter(property = "buildNumber")
    private String buildNumber;

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
     * If true then tests will not be run during a release.
     * This is the same as adding -DskipTests=true to the release goals.
     */
    @Parameter(alias = "skipTests", defaultValue = "false", property = "skipTests")
    private boolean skipTests;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        try {
            Reactor reactor = Reactor.fromProjects(projects, buildNumber);

            Git git = loadGitDir();
            List<String> tagNames = figureOutTagNamesAndThrowIfAlreadyExists(reactor.getModulesInBuildOrder(), git);

            List<File> changedFiles;
            try {
                PomUpdater pomUpdater = new PomUpdater(log, reactor);
                changedFiles = pomUpdater.updateVersion();
            } catch (IOException e) {
                throw new MojoExecutionException("Could not update the version", e);
            }
            try {
                deployReleasedProject();
            } finally {
                revertChanges(git, changedFiles);
            }
            try {
                for (String tagName : tagNames) {
                    tagRepo(tagName, git);
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Could not access the git repository. Please make sure you are releasing from a git repo.", e);
            } catch (GitAPIException e) {
                throw new MojoExecutionException("Could not tag the git repository", e);
            }
        } catch (GitAPIException gae) {
            printBigErrorMessageAndThrow(log, "Could not release due to a Git error",
                asList("There was an error while accessing the Git repository. The error returned from git was:", gae.getMessage()));
        } catch (ValidationException e) {
            printBigErrorMessageAndThrow(log, e.getMessage(), e.getMessages());
        }
    }

    private List<String> figureOutTagNamesAndThrowIfAlreadyExists(List<ReleasableModule> modules, Git git) throws GitAPIException, ValidationException {
        List<String> names = new ArrayList<String>();
        for (ReleasableModule module : modules) {
            String tag = module.getTagName();
            if (GitHelper.hasLocalTag(git, tag)) {
                String summary = "There is already a tag named " + tag + " in this repository.";
                throw new ValidationException(summary, asList(
                    summary,
                    "It is likely that this version has been released before.",
                    "Please try incrementing the build number and trying again."
                ));
            }
            names.add(tag);
        }
        return names;
    }

    private void printBigErrorMessageAndThrow(Log log, String terseMessage, List<String> linesToLog) throws MojoExecutionException {
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

    private static Git loadGitDir() throws MojoExecutionException {
        Git git;
        File gitDir = new File(".");
        try {
            git = Git.open(gitDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not open git repository. Is " + pathOf(gitDir) + " a git repository?");
        }
        return git;
    }

    private static String pathOf(File file) {
        String path;
        try {
            path = file.getCanonicalPath();
        } catch (IOException e1) {
            path = file.getAbsolutePath();
        }
        return path;
    }

    private void revertChanges(Git git, List<File> changedFiles) throws MojoExecutionException {
        boolean hasErrors = false;
        File workTree = git.getRepository().getWorkTree();
        for (File changedFile : changedFiles) {
            try {
                String pathRelativeToWorkingTree = Repository.stripWorkDir(workTree, changedFile);
                git.checkout().addPath(pathRelativeToWorkingTree).call();
            } catch (GitAPIException e) {
                hasErrors = true;
                getLog().error("Unable to revert changes to " + changedFile + " - you may need to manually revert this file. Error was: " + e.getMessage());
            }
        }
        if (hasErrors) {
            throw new MojoExecutionException("Could not revert changes - working directory is no longer clean. Please revert changes manually");
        }
    }

    private void deployReleasedProject() throws MojoExecutionException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setInteractive(false);
//        request.setPomFile( new File( "/path/to/pom.xml" ) );

        if (goals == null) {
            goals = asList("deploy");
        }
        if (skipTests) {
            goals.add("-DskipTests=true");
        }
        request.setGoals(goals);
        List<String> profiles = new ArrayList<String>();
        for (Object activatedProfile : project.getActiveProfiles()) {
            profiles.add(((org.apache.maven.model.Profile) activatedProfile).getId());
        }
        request.setProfiles(profiles);
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

    private void tagRepo(String tag, Git git) throws IOException, GitAPIException {
        getLog().info("About to tag the repository with " + tag);
        Ref tagRef = git.tag().setAnnotated(true).setName(tag).setMessage("Release " + tag).call();
        git.push().add(tagRef).call();
    }
}
