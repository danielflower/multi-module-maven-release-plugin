package com.github.danielflower.mavenplugins.release.pom;

import static java.lang.String.format;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.reactor.UnresolvedSnapshotDependencyException;

final class ReleaseContext implements Context {
	private final List<String> errors = new LinkedList<>();
	private final Reactor reactor;
	private final MavenProject project;

	ReleaseContext(final Reactor reactor, final MavenProject project) {
		this.reactor = reactor;
		this.project = project;
	}

	@Override
	public void addError(final String format, final Object... args) {
		errors.add(format(format, args));
	}

	@Override
	public MavenProject getProject() {
		return project;
	}

	@Override
	public List<String> getErrors() {
		return errors;
	}

	@Override
	public String getVersionToDependOn(final String groupId, final String artifactId, final String version)
			throws UnresolvedSnapshotDependencyException {
		return reactor.find(groupId, artifactId, version).getVersionToDependOn();
	}
}
