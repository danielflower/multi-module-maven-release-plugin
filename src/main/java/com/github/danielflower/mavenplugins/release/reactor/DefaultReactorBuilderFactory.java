package com.github.danielflower.mavenplugins.release.reactor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugin.logging.Log;

import com.github.danielflower.mavenplugins.release.version.VersionFactory;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
final class DefaultReactorBuilderFactory implements ReactorBuilderFactory {
	private final Log log;
	private final VersionFactory versionFactory;

	@com.google.inject.Inject // Compatibility: Maven 3.0.1 - 3.2.1
	@Inject // Maven 3.3.0 and greater
	DefaultReactorBuilderFactory(final Log log, final VersionFactory versionFactory) {
		this.log = log;
		this.versionFactory = versionFactory;
	}

	@Override
	public ReactorBuilder newBuilder() {
		return new DefaultReactorBuilder(log, versionFactory);
	}

}
