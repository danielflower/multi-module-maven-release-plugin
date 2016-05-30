package com.github.danielflower.mavenplugins.release.pom;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

@Component(role = PomWriterFactory.class)
class PomWriterFactory {

	@Requirement(role = SCMRepository.class)
	private SCMRepository repository;

	@Requirement(role = MavenXpp3WriterFactory.class)
	private MavenXpp3WriterFactory writerFactory;

	@Requirement(role = Log.class)
	private Log log;

	void setRepository(final SCMRepository repository) {
		this.repository = repository;
	}

	void setMavenXpp3WriterFactory(final MavenXpp3WriterFactory writerFactory) {
		this.writerFactory = writerFactory;
	}

	void setLog(final Log log) {
		this.log = log;
	}

	PomWriter newWriter() {
		final PomWriter writer = new PomWriter();
		writer.setRepository(repository);
		writer.setWriter(writerFactory.newWriter());
		writer.setLog(log);
		return writer;
	}
}
