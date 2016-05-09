package com.github.danielflower.mavenplugins.release.pom;

import static java.lang.String.format;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;

class Context {
	private final List<String> errors = new LinkedList<>();
	private final Reactor reactor;
	private final MavenProject project;
	private final String newVersion;

	Context(final Reactor reactor, final MavenProject project, final String newVersion) {
		this.reactor = reactor;
		this.project = project;
		this.newVersion = newVersion;
	}

	public void addError(final String format, final Object... args) {
		errors.add(format(format, args));
	}

	public MavenProject getProject() {
		return project;
	}

	public String getNewVersion() {
		return newVersion;
	}

	public Reactor getReactor() {
		return reactor;
	}

	public List<String> getErrors() {
		return errors;
	}
}
