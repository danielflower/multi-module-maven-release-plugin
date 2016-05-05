package com.github.danielflower.mavenplugins.release.pom;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.logging.Log;

import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

@Named
@Singleton
class PomWriterFactory {
	private final SCMRepository repository;

	@Inject
	PomWriterFactory(final SCMRepository repository) {
		this.repository = repository;
	}

	PomWriter newWriter(final Log log) {
		return new PomWriter(repository, log);
	}
}
