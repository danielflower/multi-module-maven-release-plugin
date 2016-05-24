package com.github.danielflower.mavenplugins.release.scm;

import static java.lang.String.format;
import static org.junit.Assert.fail;

import org.junit.Test;

public class GitRepositoryTest {
	private final GitRepository repository = new GitRepository();

	@Test
	public void checkValidRefName() throws SCMException {
		// This should be ok
		repository.checkValidRefName("1.0.0");

		try {
			repository.checkValidRefName("\\");
			fail("Exception expected");
		} catch (final SCMException expected) {
			expected.getMessage().equals(format(GitRepository.INVALID_REF_NAME_MESSAGE, "\\"));
		}
	}
}
