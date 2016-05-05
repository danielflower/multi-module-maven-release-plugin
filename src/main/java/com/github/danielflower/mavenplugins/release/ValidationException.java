package com.github.danielflower.mavenplugins.release;

import java.util.Arrays;
import java.util.List;

public class ValidationException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8346716149862982149L;
	private final List<String> messages;

	public ValidationException(final String summary, final List<String> messages) {
		super(summary);
		this.messages = messages;
	}

	public ValidationException(final String summary, final Throwable error) {
		super(summary, error);
		this.messages = Arrays.asList(summary, "" + error);
	}

	public List<String> getMessages() {
		return messages;
	}
}
