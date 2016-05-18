package com.github.danielflower.mavenplugins.release.substitution;

import org.apache.maven.model.Dependency;
import org.codehaus.plexus.component.annotations.Component;

/**
 * @author rolandhauser
 *
 */
@Component(role = PropertyAdapter.class, hint = "dependencyAdapter")
class DependencyAdapter implements PropertyAdapter<Dependency> {

	@Override
	public String getArtifactId(final Dependency origin) {
		return origin.getArtifactId();
	}

	@Override
	public String getGroupId(final Dependency origin) {
		return origin.getGroupId();
	}

	@Override
	public String getVersion(final Dependency origin) {
		return origin.getVersion();
	}

}