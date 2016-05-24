package com.github.danielflower.mavenplugins.release.scm;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

public class GitHelper {

	static final String REFS_TAGS = "refs/tags/";

	public static boolean hasLocalTag(final Git repo, final String tagToCheck) throws GitAPIException {
		for (final Ref ref : repo.tagList().call()) {
			final String currentTag = ref.getName().replace(REFS_TAGS, "");
			if (tagToCheck.equals(currentTag)) {
				return true;
			}
		}
		return false;
	}
}
