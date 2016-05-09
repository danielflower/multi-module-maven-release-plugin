package com.github.danielflower.mavenplugins.release.pom;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;

import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

@Named
@Singleton
class PomWriterFactory {
	private final SCMRepository repository;
	private final MavenXpp3Writer writer;
	private final Log log;

	@com.google.inject.Inject // Compatibility: Maven 3.0.1 - 3.2.1
	@Inject // Maven 3.3.0 and greater
	PomWriterFactory(final SCMRepository repository, final MavenXpp3Writer writer, final Log log) {
		this.repository = repository;
		this.writer = writer;
		this.log = log;
	}

	PomWriter newWriter() {
		return new PomWriter(repository, writer, log);
	}
}
