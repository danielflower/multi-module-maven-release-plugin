package com.github.danielflower.mavenplugins.release.pom;

@SuppressWarnings("serial")
public class ChangeSetCloseException extends POMUpdateException {

	public ChangeSetCloseException(final String format, final Object... args) {
		super(format, args);
	}

	public ChangeSetCloseException(final Throwable cause, final String format, final Object... args) {
		super(cause, format, args);
	}

}
