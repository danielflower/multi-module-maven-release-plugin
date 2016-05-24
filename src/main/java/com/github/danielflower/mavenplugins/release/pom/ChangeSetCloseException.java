package com.github.danielflower.mavenplugins.release.pom;

import com.github.danielflower.mavenplugins.release.PluginException;

@SuppressWarnings("serial")
public class ChangeSetCloseException extends PluginException {

	public ChangeSetCloseException(final String format, final Object... args) {
		super(format, args);
	}

	public ChangeSetCloseException(final Throwable cause, final String format, final Object... args) {
		super(cause, format, args);
	}

}
