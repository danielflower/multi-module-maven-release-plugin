package com.github.danielflower.mavenplugins.release.pom;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = MavenXpp3WriterFactory.class)
class MavenXpp3WriterFactory {

	public MavenXpp3Writer newWriter() {
		return new MavenXpp3Writer();
	}
}
