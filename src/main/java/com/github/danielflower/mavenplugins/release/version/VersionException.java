package com.github.danielflower.mavenplugins.release.version;

import com.github.danielflower.mavenplugins.release.PluginException;

@SuppressWarnings("serial")
public class VersionException extends PluginException {

	public VersionException(final String format, final Object... args) {
		super(format, args);
	}

	public VersionException(final Throwable cause, final String format, final Object... args) {
		super(cause, format, args);
	}

	@Override
	public VersionException add(final String format, final Object... args) {
		super.add(format, args);
		return this;
	}

}
