package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.*;

import java.util.Collections;
import java.util.List;

@Mojo(name = "release")
public class ReleaseMojo extends AbstractMojo {

    @Parameter(alias = "releaseVersion", property = "releaseVersion")
    private String releaseVersion;

    @Parameter(alias = "releaseGoals")
    private List<String> goals;

    public ReleaseMojo() {
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Release plugin running with release version " + releaseVersion);
        deployReleasedProject();
    }

    private void deployReleasedProject() throws MojoExecutionException {
        InvocationRequest request = new DefaultInvocationRequest();
//        request.setPomFile( new File( "/path/to/pom.xml" ) );

        if (goals == null) {
            goals = Collections.singletonList("deploy");
        }
        request.setGoals(goals);
        getLog().info("About to run mvn " + goals);

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
}
