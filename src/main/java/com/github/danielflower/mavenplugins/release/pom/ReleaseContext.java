package com.github.danielflower.mavenplugins.release.pom;

import static java.lang.String.format;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.reactor.ReleasableModule;
import com.github.danielflower.mavenplugins.release.reactor.UnresolvedSnapshotDependencyException;

final class ReleaseContext implements Context {
	private final List<String> errors = new LinkedList<>();
	private final Reactor reactor;
	private final MavenProject project;
	private final boolean incrementSnapshotVersionAfterRelease;

	ReleaseContext(final Reactor reactor, final MavenProject project,
			final boolean incrementSnapshotVersionAfterRelease) {
		this.reactor = reactor;
		this.project = project;
		this.incrementSnapshotVersionAfterRelease = incrementSnapshotVersionAfterRelease;
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
	public ReleasableModule getVersionToDependOn(final String groupId, final String artifactId)
			throws UnresolvedSnapshotDependencyException {
		return reactor.find(groupId, artifactId);
	}

	@Override
	public boolean incrementSnapshotVersionAfterRelease() {
		return incrementSnapshotVersionAfterRelease;
	}
}
