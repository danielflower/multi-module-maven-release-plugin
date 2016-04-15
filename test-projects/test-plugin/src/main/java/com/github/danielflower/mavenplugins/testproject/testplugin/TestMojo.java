package com.github.danielflower.mavenplugins.testproject.testplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * Dummy mojo that does nothing.
 * @since 1.5.0
 */
@Mojo(
    name = "test",
    defaultPhase = LifecyclePhase.INITIALIZE,
    threadSafe = true
)
public class TestMojo extends AbstractMojo {
    @Parameter(property = "project", required = true, readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "projects", required = true, readonly = true, defaultValue = "${reactorProjects}")
    private List<MavenProject> projects;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
      getLog().info("Hello world");
    }
}
