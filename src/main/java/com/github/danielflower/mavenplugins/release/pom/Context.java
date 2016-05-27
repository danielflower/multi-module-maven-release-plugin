package com.github.danielflower.mavenplugins.release.pom;

import java.util.List;

import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.reactor.ReleasableModule;
import com.github.danielflower.mavenplugins.release.reactor.UnresolvedSnapshotDependencyException;

interface Context {

	void addError(String format, Object... args);

	MavenProject getProject();

	List<String> getErrors();

	ReleasableModule getVersionToDependOn(String groupId, String artifactId)
			throws UnresolvedSnapshotDependencyException;

	boolean incrementSnapshotVersionAfterRelease();
}
