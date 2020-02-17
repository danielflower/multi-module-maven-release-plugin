package com.github.danielflower.mavenplugins.testproject.localplugin.pluginproject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo( name = "helloworld")
public class MyMojo extends AbstractMojo {
    public void execute() throws MojoExecutionException, MojoFailureException {

    }
}
