package com.github.danielflower.mavenplugins.release.pom;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import com.google.inject.Provider;

@Named
@Singleton
class MavenXpp3WriterProvider implements Provider<MavenXpp3Writer> {
	private final MavenXpp3Writer writer = new MavenXpp3Writer();

	@Override
	public MavenXpp3Writer get() {
		return writer;
	}

}
