package com.github.danielflower.mavenplugins.release.scm;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.ValidationException;

public interface SCMRepository {

	void errorIfNotClean() throws ValidationException;

	List<String> remoteTagsFrom(List<AnnotatedTag> annotatedTags) throws ValidationException, GitAPIException;

	boolean hasLocalTag(String tag) throws ValidationException, GitAPIException;

	void tagRepoAndPush(AnnotatedTag tag) throws ValidationException, GitAPIException;

	boolean revertChanges(List<File> changedFiles) throws ValidationException, IOException;

	Collection<Long> getRemoteBuildNumbers(String artifactId, String versionWithoutBuildNumber)
			throws ValidationException, GitAPIException;

	List<AnnotatedTag> tagsForVersion(String module, String versionWithoutBuildNumber)
			throws ValidationException, MojoExecutionException;

	DiffDetector newDiffDetector() throws ValidationException;
}
