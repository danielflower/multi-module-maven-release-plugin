package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "release")
public class ReleaseMojo extends AbstractMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Hello there world");
//        InvocationRequest request = new DefaultInvocationRequest();
////        request.setPomFile( new File( "/path/to/pom.xml" ) );
//        request.setGoals( Collections.singletonList("install") );
//
//        Invoker invoker = new DefaultInvoker();
//        try {
//            InvocationResult result = invoker.execute(request);
//            getLog().info(result.toString());
//        } catch (MavenInvocationException e) {
//            e.printStackTrace();
//        }
    }
}
