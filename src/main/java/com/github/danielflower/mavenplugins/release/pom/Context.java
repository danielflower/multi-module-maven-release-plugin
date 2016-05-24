package com.github.danielflower.mavenplugins.release.pom;

import java.util.List;

import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.reactor.UnresolvedSnapshotDependencyException;

interface Context {

	void addError(final String format, final Object... args);

	MavenProject getProject();

	List<String> getErrors();

	String getVersionToDependOn(final String groupId, final String artifactId, final String version)
			throws UnresolvedSnapshotDependencyException;
}
