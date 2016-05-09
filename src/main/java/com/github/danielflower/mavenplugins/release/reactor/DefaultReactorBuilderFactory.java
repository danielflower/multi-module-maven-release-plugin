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

	@Inject
	DefaultReactorBuilderFactory(final Log log, final VersionFactory versionFactory) {
		this.log = log;
		this.versionFactory = versionFactory;
	}

	@Override
	public ReactorBuilder newBuilder() {
		return new DefaultReactorBuilder(log, versionFactory);
	}

}
