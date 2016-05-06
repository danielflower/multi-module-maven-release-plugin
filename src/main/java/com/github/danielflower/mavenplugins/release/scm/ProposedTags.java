package com.github.danielflower.mavenplugins.release.scm;

import java.util.List;

import javax.xml.bind.ValidationException;

import org.eclipse.jgit.api.errors.GitAPIException;

public interface ProposedTags extends Iterable<ProposedTag> {

	ProposedTag getTag(String tag, String version, long buildNumber) throws ValidationException;

	/**
	 * Will be removed when LocalGitRepoTest is refactored
	 * 
	 * @deprecated
	 */
	@Deprecated
	List<String> getMatchingRemoteTags() throws GitAPIException;

	void tagAndPushRepo() throws GitAPIException;
}
