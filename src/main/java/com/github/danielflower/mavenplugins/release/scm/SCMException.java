package com.github.danielflower.mavenplugins.release.scm;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import com.github.danielflower.mavenplugins.release.PluginException;

/**
 *
 */
@SuppressWarnings("serial")
public class SCMException extends PluginException {

	public SCMException(final Throwable cause, final String format, final Object... args) {
		super(cause, format, args);
	}

	public SCMException(final String format, final Object... args) {
		super(format, args);
	}

	@Override
	public SCMException add(final String format, final Object... args) {
		super.add(format, args);
		return this;
	}

	@Override
	protected void printBigErrorMessageAndThrow(final Log log) throws MojoExecutionException {
		final StringWriter sw = new StringWriter();
		printStackTrace(new PrintWriter(sw));
		final String exceptionAsString = sw.toString();
		add("Could not release due to a Git error");
		add("There was an error while accessing the Git repository. The error returned from git was:");
		add("Stack trace:");
		add(exceptionAsString);
		super.printBigErrorMessageAndThrow(log);
	}

}
