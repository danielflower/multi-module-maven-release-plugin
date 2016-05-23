package com.github.danielflower.mavenplugins.release;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;

import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("serial")
public abstract class PluginException extends Exception {
	private final List<String> messages = new LinkedList<>();

	public PluginException(final Throwable cause, final String format, final Object... args) {
		super(format(format, args), cause);
		messages.add(getMessage());
	}

	public PluginException(final String format, final Object... args) {
		super(format(format, args));
		messages.add(getMessage());
	}

	protected final void addLine(final String format, final Object... args) {
		messages.add(format(format, args));
	}

	public abstract PluginException add(final String format, final Object... args);

	public List<String> getMessages() {
		return unmodifiableList(messages);
	}

}
