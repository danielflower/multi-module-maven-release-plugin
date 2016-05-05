package com.github.danielflower.mavenplugins.release.pom;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.junit.Test;

public class MavenXpp3WriterProviderTest {
	private final MavenXpp3WriterProvider provider = new MavenXpp3WriterProvider();

	@Test
	public void verifyGet() {
		final MavenXpp3Writer writer = provider.get();
		assertNotNull(writer);
		assertSame(writer, provider.get());
	}
}
