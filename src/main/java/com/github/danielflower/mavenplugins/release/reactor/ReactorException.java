package com.github.danielflower.mavenplugins.release.reactor;

import com.github.danielflower.mavenplugins.release.PluginException;

@SuppressWarnings("serial")
public class ReactorException extends PluginException {

	public ReactorException(final Throwable cause, final String message, final Object... args) {
		super(cause, message, args);
	}

	public ReactorException(final String message, final Object... args) {
		super(message, args);
	}

	@Override
	public ReactorException add(final String format, final Object... args) {
		addLine(format, args);
		return this;
	}

}
