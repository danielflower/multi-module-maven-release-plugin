package com.github.danielflower.mavenplugins.release.log;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = LogHolder.class)
final class DefaultLogHolder implements LogHolder {
	private Log log;

	@Override
	public void setLog(final Log log) {
		this.log = log;
	}

	@Override
	public Log getLog() {
		if (log == null) {
			throw new IllegalStateException("Delegate logger has not been set!");
		}
		return log;
	}
}
