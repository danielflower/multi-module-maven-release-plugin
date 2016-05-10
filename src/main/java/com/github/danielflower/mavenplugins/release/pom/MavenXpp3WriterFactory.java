package com.github.danielflower.mavenplugins.release.pom;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

@Named
@Singleton
class MavenXpp3WriterFactory {

	public MavenXpp3Writer newWriter() {
		return new MavenXpp3Writer();
	}
}
