package com.github.danielflower.mavenplugins.release.scm;

public interface ProposedTagsBuilder {

	ProposedTagsBuilder add(String tag, String version, long buildNumber) throws SCMException;

	ProposedTags build() throws SCMException;
}
