package com.github.danielflower.mavenplugins.release.pom;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * @author rolandhauser
 *
 */
abstract class Command {

	@Requirement(role = Log.class)
	protected Log log;

	void setCommand(final Log log) {
		this.log = log;
	}

	static boolean isSnapshot(final String version) {
		return version != null && version.endsWith("-SNAPSHOT");
	}

	public abstract void alterModel(Context updateContext);
}
