package com.github.danielflower.mavenplugins.release.pom;

import static java.lang.String.format;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.reactor.ReleasableModule;
import com.github.danielflower.mavenplugins.release.reactor.UnresolvedSnapshotDependencyException;

class Context {

	private final List<String> errors = new LinkedList<>();
	private final Reactor reactor;
	private final MavenProject project;
	private final boolean incrementSnapshotVersionAfterRelease;

	Context(final Reactor reactor, final MavenProject project, final boolean incrementSnapshotVersionAfterRelease) {
		this.reactor = reactor;
		this.project = project;
		this.incrementSnapshotVersionAfterRelease = incrementSnapshotVersionAfterRelease;
	}

	public void addError(final String format, final Object... args) {
		errors.add(format(format, args));
	}

	public MavenProject getProject() {
		return project;
	}

	public List<String> getErrors() {
		return errors;
	}

	public ReleasableModule getVersionToDependOn(final String groupId, final String artifactId)
			throws UnresolvedSnapshotDependencyException {
		return reactor.find(groupId, artifactId);
	}

	public boolean incrementSnapshotVersionAfterRelease() {
		return incrementSnapshotVersionAfterRelease;
	}
}
