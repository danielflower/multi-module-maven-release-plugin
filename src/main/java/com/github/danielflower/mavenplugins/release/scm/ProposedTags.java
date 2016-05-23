package com.github.danielflower.mavenplugins.release.scm;

import java.util.List;

public interface ProposedTags extends Iterable<ProposedTag> {

	ProposedTag getTag(String tag, String version, long buildNumber) throws SCMException;

	/**
	 * Will be removed when LocalGitRepoTest is refactored
	 * 
	 * @throws SCMException
	 * 
	 * @deprecated
	 */
	@Deprecated
	List<String> getMatchingRemoteTags() throws SCMException;

	void tagAndPushRepo() throws SCMException;
}
