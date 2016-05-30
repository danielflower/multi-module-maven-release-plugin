package com.github.danielflower.mavenplugins.release.version;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Default implementation of the {@link VersionBuilderFactory} interface.
 *
 */
@Component(role = VersionBuilderFactory.class)
final class DefaultVersionFactory implements VersionBuilderFactory {
	static final String SNAPSHOT_EXTENSION = "-SNAPSHOT";

	@Requirement(role = BuildNumberFinder.class)
	private BuildNumberFinder finder;

	@Requirement(role = ChangeDetectorFactory.class)
	private ChangeDetectorFactory detectorFactory;

	void setFinder(final BuildNumberFinder finder) {
		this.finder = finder;
	}

	void setDetectorFactory(final ChangeDetectorFactory detectorFactory) {
		this.detectorFactory = detectorFactory;
	}

	@Override
	public VersionBuilder newBuilder() {
		return new DefaultVersionBuilder(finder, detectorFactory);
	}
}
