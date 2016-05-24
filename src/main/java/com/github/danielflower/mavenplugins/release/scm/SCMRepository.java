package com.github.danielflower.mavenplugins.release.scm;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.lib.Ref;

public interface SCMRepository {

	ProposedTag fromRef(Ref gitTag) throws SCMException;

	void errorIfNotClean() throws SCMException;

	boolean hasLocalTag(String tag) throws SCMException;

	void revertChanges(List<File> changedFiles) throws SCMException;

	Collection<Long> getRemoteBuildNumbers(String remoteUrl, String artifactId, String versionWithoutBuildNumber)
			throws SCMException;

	List<ProposedTag> tagsForVersion(String module, String versionWithoutBuildNumber) throws SCMException;

	ProposedTagsBuilder newProposedTagsBuilder(String remoteUrl) throws SCMException;

	void checkValidRefName(String releaseVersion) throws SCMException;

	boolean hasChangedSince(String modulePath, List<String> childModules, Collection<ProposedTag> tags)
			throws SCMException;
}
