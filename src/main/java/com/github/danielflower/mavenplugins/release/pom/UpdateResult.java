package com.github.danielflower.mavenplugins.release.pom;

import java.io.File;
import java.util.List;

class UpdateResult {
	public final List<File> alteredPoms;
	public final List<String> dependencyErrors;
	public final Exception unexpectedException;

	public UpdateResult(final List<File> alteredPoms, final List<String> dependencyErrors,
			final Exception unexpectedException) {
		this.alteredPoms = alteredPoms;
		this.dependencyErrors = dependencyErrors;
		this.unexpectedException = unexpectedException;
	}

	public boolean success() {
		return (dependencyErrors.size() == 0) && (unexpectedException == null);
	}
}