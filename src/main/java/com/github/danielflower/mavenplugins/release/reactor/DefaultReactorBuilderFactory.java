package com.github.danielflower.mavenplugins.release.reactor;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.github.danielflower.mavenplugins.release.scm.SCMRepository;
import com.github.danielflower.mavenplugins.release.version.VersionFactory;

/**
 * @author rolandhauser
 *
 */
@Component(role = ReactorBuilderFactory.class)
final class DefaultReactorBuilderFactory implements ReactorBuilderFactory {

	@Requirement(role = Log.class)
	private Log log;

	@Requirement(role = VersionFactory.class)
	private VersionFactory versionFactory;

	@Requirement(role = SCMRepository.class)
	private SCMRepository repository;

	void setLog(final Log log) {
		this.log = log;
	}

	void setVersionFactory(final VersionFactory versionFactory) {
		this.versionFactory = versionFactory;
	}

	void setRepository(final SCMRepository repository) {
		this.repository = repository;
	}

	@Override
	public ReactorBuilder newBuilder() {
		return new DefaultReactorBuilder(log, repository, versionFactory);
	}

}
