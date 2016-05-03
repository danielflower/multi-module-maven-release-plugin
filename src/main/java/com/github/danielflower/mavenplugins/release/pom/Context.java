package com.github.danielflower.mavenplugins.release.pom;

import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;

/**
 * @author rolandhauser
 *
 */
public interface Context {

	void addError(String format, Object... args);

	MavenProject getProject();

	String getNewVersion();

	Reactor getReactor();

	void debug(String format, Object... args);
}
