package com.github.danielflower.mavenplugins.release.scm;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import com.github.danielflower.mavenplugins.release.ValidationException;

public interface SCMRepository {

	ProposedTag fromRef(Ref gitTag) throws IOException;

	void errorIfNotClean() throws ValidationException;

	boolean hasLocalTag(String tag) throws ValidationException, GitAPIException;

	boolean revertChanges(List<File> changedFiles) throws ValidationException, IOException;

	Collection<Long> getRemoteBuildNumbers(String artifactId, String versionWithoutBuildNumber)
			throws ValidationException, GitAPIException;

	List<ProposedTag> tagsForVersion(String module, String versionWithoutBuildNumber)
			throws ValidationException, MojoExecutionException;

	DiffDetector newDiffDetector() throws ValidationException;

	ProposedTagsBuilder newProposedTagsBuilder();
}
