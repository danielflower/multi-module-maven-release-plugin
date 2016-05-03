package com.github.danielflower.mavenplugins.release.pom;

/**
 * @author rolandhauser
 *
 */
abstract class Command {

	protected final boolean isSnapshot(final String version) {
		return (version != null && version.endsWith("-SNAPSHOT"));
	}

	public abstract void alterModel(Context updateContext);
}
