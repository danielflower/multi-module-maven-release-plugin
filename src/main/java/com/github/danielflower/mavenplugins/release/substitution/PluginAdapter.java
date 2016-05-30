package com.github.danielflower.mavenplugins.release.substitution;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.component.annotations.Component;

/**
 * @author rolandhauser
 *
 */
@Component(role = PropertyAdapter.class, hint = "pluginAdapter")
final class PluginAdapter implements PropertyAdapter<Plugin> {

	@Override
	public String getArtifactId(final Plugin origin) {
		return origin.getArtifactId();
	}

	@Override
	public String getGroupId(final Plugin origin) {
		return origin.getGroupId();
	}

	@Override
	public String getVersion(final Plugin origin) {
		return origin.getVersion();
	}

}