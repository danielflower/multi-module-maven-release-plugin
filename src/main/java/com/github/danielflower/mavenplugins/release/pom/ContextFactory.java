package com.github.danielflower.mavenplugins.release.pom;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;

@Component(role = ContextFactory.class)
class ContextFactory {

	Context newReleaseContext(final Reactor reactor, final MavenProject project,
			final boolean incrementSnapshotVersionAfterRelease) {
		return new ReleaseContext(reactor, project.clone(), incrementSnapshotVersionAfterRelease);
	}
}
