package com.github.danielflower.mavenplugins.release.pom;

import org.apache.maven.plugin.logging.Log;

/**
 * @author rolandhauser
 *
 */
abstract class Command {
	protected final Log log;

	protected Command(final Log log) {
		this.log = log;
	}

	static boolean isSnapshot(final String version) {
		return version != null && version.endsWith("-SNAPSHOT");
	}

	public abstract void alterModel(Context updateContext);
}
