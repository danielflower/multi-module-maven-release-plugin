package com.github.danielflower.mavenplugins.release.scm;

import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.ValidationException;

public interface ProposedTags extends Iterable<ProposedTag> {

	ProposedTag getTag(String tag, String version, long buildNumber) throws ValidationException;

	/**
	 * Will be removed when LocalGitRepoTest is refactored
	 * 
	 * @throws ValidationException
	 * 
	 * @deprecated
	 */
	@Deprecated
	List<String> getMatchingRemoteTags() throws GitAPIException, ValidationException;

	void tagAndPushRepo() throws GitAPIException;
}
