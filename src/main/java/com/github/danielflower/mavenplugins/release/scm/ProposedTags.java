package com.github.danielflower.mavenplugins.release.scm;

import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;

public interface ProposedTags {

	ProposedTag add(String tag, String version, long buildNumber);

	List<String> getMatchingRemoteTags() throws GitAPIException;

	void tagAndPushRepo() throws GitAPIException;
}
