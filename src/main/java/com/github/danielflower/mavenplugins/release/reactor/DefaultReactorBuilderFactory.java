package com.github.danielflower.mavenplugins.release.reactor;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
final class DefaultReactorBuilderFactory implements ReactorBuilderFactory {

	@Override
	public ReactorBuilder newBuilder() {
		return new DefaultReactorBuilder();
	}

}
