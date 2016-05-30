package com.github.danielflower.mavenplugins.release.reactor;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.github.danielflower.mavenplugins.release.version.VersionBuilderFactory;

/**
 * @author rolandhauser
 *
 */
@Component(role = ReactorBuilderFactory.class)
final class DefaultReactorBuilderFactory implements ReactorBuilderFactory {

	@Requirement(role = Log.class)
	private Log log;

	@Requirement(role = VersionBuilderFactory.class)
	private VersionBuilderFactory versionFactory;

	void setLog(final Log log) {
		this.log = log;
	}

	void setVersionFactory(final VersionBuilderFactory versionFactory) {
		this.versionFactory = versionFactory;
	}

	@Override
	public ReactorBuilder newBuilder() {
		return new DefaultReactorBuilder(log, versionFactory);
	}

}
