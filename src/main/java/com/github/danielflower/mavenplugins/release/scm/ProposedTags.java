package com.github.danielflower.mavenplugins.release.scm;

public interface ProposedTags extends Iterable<ProposedTag> {

	ProposedTag getTag(String tag, String version, long buildNumber) throws SCMException;

	void tagAndPushRepo() throws SCMException;
}
