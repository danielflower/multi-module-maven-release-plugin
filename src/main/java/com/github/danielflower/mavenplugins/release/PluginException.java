package com.github.danielflower.mavenplugins.release;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

@SuppressWarnings("serial")
public class PluginException extends Exception {
	protected final List<String> messages = new LinkedList<>();

	public PluginException(final Throwable cause, final String format, final Object... args) {
		super(format(format, args), cause);
	}

	public PluginException(final String format, final Object... args) {
		super(format(format, args));
	}

	public PluginException add(final String format, final Object... args) {
		messages.add(format(format, args));
		return this;
	}

	public List<String> getMessages() {
		return unmodifiableList(messages);
	}

	private void printCause(final Log log) {
		log.error("");
		log.error(format("Caused by %s", getCause().getClass()));
		log.error(getCause().getMessage());

		if (getCause() instanceof PluginException) {
			final PluginException plex = (PluginException) getCause();
			for (final String line : plex.messages) {
				log.error(line);
			}
			log.error("");
			plex.printCause(log);
		}
	}

	protected void printBigErrorMessageAndThrow(final Log log) throws MojoExecutionException {
		log.error("");
		log.error("");
		log.error("");
		log.error("************************************");
		log.error("Could not execute the release plugin");
		log.error("************************************");
		log.error("");
		log.error("");
		log.error(getMessage());
		for (final String line : messages) {
			log.error(line);
		}
		log.error("");
		log.error("");
		printCause(log);
		throw new MojoExecutionException(getMessage());
	}

}
