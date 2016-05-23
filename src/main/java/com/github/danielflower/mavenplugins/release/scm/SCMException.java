package com.github.danielflower.mavenplugins.release.scm;

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
		addLine(format, args);
		return this;
	}

}
