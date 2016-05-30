package com.github.danielflower.mavenplugins.release.version;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

@Component(role = ChangeDetectorFactory.class)
class ChangeDetectorFactory {

	@Requirement(role = Log.class)
	private Log log;

	@Requirement(role = SCMRepository.class)
	private SCMRepository repository;

	void setLog(final Log log) {
		this.log = log;
	}

	void setRepository(final SCMRepository repository) {
		this.repository = repository;
	}

	ChangeDetector newDetector() {
		return new ChangeDetector(log, repository);
	}
}
