package com.github.danielflower.mavenplugins.release.scm;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.ValidationException;

public interface SCMRepository {

	void errorIfNotClean() throws ValidationException;

	List<String> remoteTagsFrom(List<AnnotatedTag> annotatedTags) throws GitAPIException;

	boolean hasLocalTag(String tag) throws GitAPIException;

	void tagRepoAndPush(AnnotatedTag tag) throws GitAPIException;

	boolean revertChanges(Log log, List<File> changedFiles) throws IOException;

	Collection<Long> getRemoteBuildNumbers(String artifactId, String versionWithoutBuildNumber) throws GitAPIException;

	List<AnnotatedTag> tagsForVersion(String module, String versionWithoutBuildNumber) throws MojoExecutionException;

	DiffDetector newDiffDetector();
}
