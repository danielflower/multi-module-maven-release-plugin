package com.github.danielflower.mavenplugins.release.scm;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.ValidationException;

public interface ProposedTagsBuilder {

	ProposedTagsBuilder add(String tag, String version, long buildNumber) throws ValidationException, GitAPIException;

	ProposedTags build() throws ValidationException, GitAPIException;
}
