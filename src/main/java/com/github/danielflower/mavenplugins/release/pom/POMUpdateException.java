package com.github.danielflower.mavenplugins.release.pom;

import com.github.danielflower.mavenplugins.release.PluginException;

@SuppressWarnings("serial")
public class POMUpdateException extends PluginException {

	public POMUpdateException(final String format, final Object... args) {
		super(format, args);
	}

	public POMUpdateException(final Throwable cause, final String format, final Object... args) {
		super(cause, format, args);
	}

	@Override
	public PluginException add(final String format, final Object... args) {
		addLine(format, args);
		return this;
	}

}
