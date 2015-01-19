package com.github.danielflower.mavenplugins.release;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

/**
 * Releases the project.
 */
@Mojo(name = "release")
public class ReleaseMojo extends AbstractMojo {

    /**
     * The Maven Project.
     */
    @Parameter(property = "project", required = true, readonly = true, defaultValue = "${project}")
    private MavenProject project;

    /**
     * The release part of the version number to release. Given a snapshot version of "1.0-SNAPSHOT"
     * and a releaseVersion value of "2", the actual released version will be "1.0.2". This can be
     * specified using a command line parameter ("-DreleaseVersion=2") or in this plugin's configuration.
     */
    @Parameter(alias = "releaseVersion", property = "releaseVersion")
    private String releaseVersion;

    /**
     * The goals to run against the project during a release. By default this is "deploy" which
     * means the release version of your artifact will be tested and deployed.
     */
    @Parameter(alias = "releaseGoals")
    private List<String> goals;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String newVersion = project.getVersion().replace("-SNAPSHOT", "").concat(".").concat(releaseVersion);

        Git git = loadGitDir();

        List<String> changedFiles;
        try {
            changedFiles = updateVersion(project.getFile(), newVersion);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not update the version", e);
        }
        try {
            deployReleasedProject();
        } finally {
            revertChanges(git, changedFiles);
        }
        try {
            tagRepo(project.getArtifactId() + "-" + newVersion, git);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not access the git repository. Please make sure you are releasing from a git repo.", e);
        } catch (GitAPIException e) {
            throw new MojoExecutionException("Could not tag the git repository", e);
        }
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
        String gitPath;
        try {
            gitPath = file.getCanonicalPath();
        } catch (IOException e1) {
            gitPath = file.getAbsolutePath();
        }
        return gitPath;
    }

    private void revertChanges(Git git, List<String> changedFiles) throws MojoExecutionException {
        boolean hasErrors = false;
        for (String changedFile : changedFiles) {
            try {
                git.checkout().addPath(changedFile).call();
            } catch (GitAPIException e) {
                hasErrors = true;
                getLog().error("Unable to revert changes to " + changedFile + " - you may need to manually revert this file. Error was: " + e.getMessage());
            }
        }
        if (hasErrors) {
            throw new MojoExecutionException("Could not revert changes - working directory is no longer clean. Please revert changes manually");
        }
    }

    private List<String> updateVersion(File pom, String newVersion) throws IOException {
        getLog().info("Going to release " + project.getArtifactId() + " " + newVersion);

        project.getOriginalModel().setVersion(newVersion);
        Writer fileWriter = new FileWriter(pom);

        try {
            MavenXpp3Writer pomWriter = new MavenXpp3Writer();
            pomWriter.write( fileWriter, project.getOriginalModel() );
        } finally {
            fileWriter.close();
        }

        return Collections.singletonList("pom.xml");
    }

    private void deployReleasedProject() throws MojoExecutionException {
        InvocationRequest request = new DefaultInvocationRequest();
//        request.setPomFile( new File( "/path/to/pom.xml" ) );

        if (goals == null) {
            goals = Collections.singletonList("deploy");
        }
        request.setGoals(goals);
        getLog().info("About to run mvn " + goals);
        System.out.println("project = " + project);

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
