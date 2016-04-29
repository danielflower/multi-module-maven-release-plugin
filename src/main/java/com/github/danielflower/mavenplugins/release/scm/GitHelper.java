package com.github.danielflower.mavenplugins.release.scm;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import com.github.danielflower.mavenplugins.release.ValidationException;

public class GitHelper {
	public static boolean hasLocalTag(final Git repo, final String tagToCheck) throws GitAPIException {
		return tag(repo, new EqualsMatcher(tagToCheck)) != null;
	}

	public static Ref refStartingWith(final Git repo, final String tagPrefix) throws GitAPIException {
		return tag(repo, new Matcher() {
			@Override
			public boolean matches(final String tagName) {
				return tagName.startsWith(tagPrefix);
			}
		});
	}

	private static Ref tag(final Git repo, final Matcher matcher) throws GitAPIException {
		for (final Ref ref : repo.tagList().call()) {
			final String currentTag = ref.getName().replace("refs/tags/", "");
			if (matcher.matches(currentTag)) {
				return ref;
			}
		}
		return null;
	}

	public static String scmUrlToRemote(final String scmUrl) throws ValidationException {
		final String GIT_PREFIX = "scm:git:";
		if (!scmUrl.startsWith(GIT_PREFIX)) {
			final List<String> messages = new ArrayList<String>();
			final String summary = "Cannot run the release plugin with a non-Git version control system";
			messages.add(summary);
			messages.add("The value in your scm tag is " + scmUrl);
			throw new ValidationException(summary + " " + scmUrl, messages);
		}
		String remote = scmUrl.substring(GIT_PREFIX.length());
		remote = remote.replace("file://localhost/", "file:///");
		return remote;
	}

	private interface Matcher {
		public boolean matches(String tagName);
	}

	private static class EqualsMatcher implements Matcher {
		private final String tagToCheck;

		public EqualsMatcher(final String tagToCheck) {
			this.tagToCheck = tagToCheck;
		}

		@Override
		public boolean matches(final String tagName) {
			return tagToCheck.equals(tagName);
		}
	}
}
