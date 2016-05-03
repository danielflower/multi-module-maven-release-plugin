package com.github.danielflower.mavenplugins.release.pom;

import static java.lang.String.format;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;

class DefaultContext implements Context {
	private final List<String> errors = new LinkedList<>();
	private final Log log;
	private final Reactor reactor;
	private final MavenProject project;
	private final String newVersion;

	DefaultContext(final Log log, final Reactor reactor, final MavenProject project, final String newVersion) {
		this.log = log;
		this.reactor = reactor;
		this.project = project;
		this.newVersion = newVersion;
	}

	@Override
	public void addError(final String format, final Object... args) {
		errors.add(format(format, args));
	}

	@Override
	public void debug(final String format, final Object... args) {
		log.debug(format(format, args));
	}

	@Override
	public MavenProject getProject() {
		return project;
	}

	@Override
	public String getNewVersion() {
		return newVersion;
	}

	@Override
	public Reactor getReactor() {
		return reactor;
	}

	List<String> getErrors() {
		return errors;
	}
}
