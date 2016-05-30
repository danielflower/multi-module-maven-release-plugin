package com.github.danielflower.mavenplugins.release.scm;

import com.github.danielflower.mavenplugins.release.version.Version;

public interface ProposedTags extends Iterable<ProposedTag> {

	ProposedTag getTag(String tag, Version version) throws SCMException;

	void tagAndPushRepo() throws SCMException;
}
