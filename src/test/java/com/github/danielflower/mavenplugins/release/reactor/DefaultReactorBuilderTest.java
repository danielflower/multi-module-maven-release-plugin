package com.github.danielflower.mavenplugins.release.reactor;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.AnnotatedTag;
import com.github.danielflower.mavenplugins.release.DiffDetector;

public class DefaultReactorBuilderTest {

	@Test
	public void returnsTheLatestTagIfThereAreChanges() throws MojoExecutionException {
		AnnotatedTag onePointNine = AnnotatedTag.create("whatever-1.1.9", "1.1", 9);
		AnnotatedTag onePointTen = AnnotatedTag.create("whatever-1.1.10", "1.1", 10);
		assertThat(DefaultReactorBuilder.hasChangedSinceLastRelease(asList(onePointNine, onePointTen), new NeverChanged(),
				new MavenProject(), "whatever"), is(onePointTen));
		assertThat(DefaultReactorBuilder.hasChangedSinceLastRelease(asList(onePointTen, onePointNine), new NeverChanged(),
				new MavenProject(), "whatever"), is(onePointTen));
	}

	private static class NeverChanged implements DiffDetector {
		@Override
		public boolean hasChangedSince(String modulePath, List<String> childModules, Collection<AnnotatedTag> tags)
				throws IOException {
			return false;
		}
	}
}
