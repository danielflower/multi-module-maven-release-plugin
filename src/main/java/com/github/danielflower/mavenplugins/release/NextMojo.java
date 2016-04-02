package com.github.danielflower.mavenplugins.release;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Logs the versions of the modules that the releaser will release on the next release. Does not run the build nor
 * tag the repo.
 * @since 1.4.0
 */
@Mojo(
    name = "next",
    requiresDirectInvocation = true, // this should not be bound to a phase as this plugin starts a phase itself
    inheritByDefault = true, // so you can configure this in a shared parent pom
    requiresProject = true, // this can only run against a maven project
    aggregator = true // the plugin should only run once against the aggregator pom
)
public class NextMojo extends AbstractMojo {

    /**
     * The Maven Project.
     */
    @Parameter(property = "project", required = true, readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "projects", required = true, readonly = true, defaultValue = "${reactorProjects}")
    private List<MavenProject> projects;

    /**
     * <p>
     * An optional build number. See the release goal for more information.
     * </p>
     */
    @Parameter(property = "buildNumber")
    private Long buildNumber;

    /**
     * See the release goal for more information.
     */
    @Parameter(alias = "modulesToRelease", property = "modulesToRelease")
    private List<String> modulesToRelease;

    /**
     * See the release goal for more information.
     */
    @Parameter(alias = "forceRelease", property = "forceRelease")
    private List<String> modulesToForceRelease;

    @Parameter(property = "disableSshAgent")
    private boolean disableSshAgent;
    
    @Parameter(property = "knownHosts")
    private String knownHosts;
    
    @Parameter(property = "identityFile")
    private String identityFile;
    
    @Parameter(property = "passphrase")
    private String passphrase;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();

        try {
            configureJsch(log);

            LocalGitRepo repo = LocalGitRepo.fromCurrentDir(getRemoteUrlOrNullIfNoneSet(project.getScm()));
            Reactor reactor = Reactor.fromProjects(log, repo, project, projects, buildNumber, modulesToForceRelease);
            figureOutTagNamesAndThrowIfAlreadyExists(reactor.getModulesInBuildOrder(), repo, modulesToRelease);

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
            JschConfigSessionFactory.setInstance(new SshAgentSessionFactory(log, knownHosts, identityFile, passphrase));
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



}
