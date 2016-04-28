package com.github.danielflower.mavenplugins.release.reactor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.github.danielflower.mavenplugins.release.version.VersionFactory;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
final class DefaultReactorBuilderFactory implements ReactorBuilderFactory {
	private final VersionFactory versionFactory;

	@Inject
	DefaultReactorBuilderFactory(final VersionFactory versionFactory) {
		this.versionFactory = versionFactory;
	}

	@Override
	public ReactorBuilder newBuilder() {
		return new DefaultReactorBuilder(versionFactory);
	}

}
