package com.github.danielflower.mavenplugins.release.pom;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;

@Named
@Singleton
class ContextFactory {

	Context newContext(final Log log, final Reactor reactor, final MavenProject project, final String newVersion) {
		return new Context(log, reactor, project, newVersion);
	}
}
