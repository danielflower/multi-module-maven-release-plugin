package com.github.danielflower.mavenplugins.release.scm;

import com.github.danielflower.mavenplugins.release.version.Version;

public interface ProposedTagsBuilder {

	ProposedTagsBuilder add(String tag, Version version) throws SCMException;

	ProposedTags build() throws SCMException;
}
