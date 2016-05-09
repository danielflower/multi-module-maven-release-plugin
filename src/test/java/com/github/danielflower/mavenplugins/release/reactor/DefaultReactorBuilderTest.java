package com.github.danielflower.mavenplugins.release.reactor;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.github.danielflower.mavenplugins.release.scm.DiffDetector;
import com.github.danielflower.mavenplugins.release.scm.ProposedTag;

// FIXME: This test should be moved to DefaultVersion
public class DefaultReactorBuilderTest {

	/*
	 * @Test public void returnsTheLatestTagIfThereAreChanges() throws
	 * MojoExecutionException { AnnotatedTag onePointNine =
	 * AnnotatedTag.create("whatever-1.1.9", "1.1", 9); AnnotatedTag onePointTen
	 * = AnnotatedTag.create("whatever-1.1.10", "1.1", 10);
	 * assertThat(DefaultReactorBuilder.hasChangedSinceLastRelease(asList(
	 * onePointNine, onePointTen), new NeverChanged(), new MavenProject(),
	 * "whatever"), is(onePointTen));
	 * assertThat(DefaultReactorBuilder.hasChangedSinceLastRelease(asList(
	 * onePointTen, onePointNine), new NeverChanged(), new MavenProject(),
	 * "whatever"), is(onePointTen)); }
	 */

	private static class NeverChanged implements DiffDetector {
		@Override
		public boolean hasChangedSince(final String modulePath, final List<String> childModules,
				final Collection<ProposedTag> tags) throws IOException {
			return false;
		}
	}
}
