package com.github.danielflower.mavenplugins.release.scm;

import static com.github.danielflower.mavenplugins.release.scm.DefaultProposedTag.BUILD_NUMBER;
import static com.github.danielflower.mavenplugins.release.scm.DefaultProposedTag.VERSION;
import static com.github.danielflower.mavenplugins.release.scm.DefaultProposedTags.toKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.json.simple.JSONObject;

import com.github.danielflower.mavenplugins.release.version.Version;

final class DefaultProposedTagsBuilder implements ProposedTagsBuilder {
	private final Map<String, ProposedTag> proposedTags = new LinkedHashMap<>();
	private final Log log;
	private final Git git;
	private final GitRepository repo;
	private final String remoteUrl;

	DefaultProposedTagsBuilder(final Log log, final Git git, final GitRepository repo, final String remoteUrl) {
		this.log = log;
		this.git = git;
		this.repo = repo;
		this.remoteUrl = remoteUrl;
	}

	@Override
	public ProposedTagsBuilder add(final String tag, final Version version) throws SCMException {
		if (repo.hasLocalTag(tag)) {
			throw new SCMException("There is already a tag named %s in this repository.", tag)
					.add("It is likely that this version has been released before.")
					.add("Please try incrementing the build number and trying again.");
		}
		final JSONObject message = new JSONObject();
		message.put(VERSION, version.getBusinessVersion());
		message.put(BUILD_NUMBER, String.valueOf(version.getBuildNumber()));
		final ProposedTag proposedTag = new DefaultProposedTag(git, log, null, tag, message);
		proposedTags.put(toKey(tag, version), new DefaultProposedTag(git, log, null, tag, message));
		return this;
	}

	private List<String> getMatchingRemoteTags() throws SCMException {
		final List<String> tagNamesToSearchFor = new ArrayList<String>();
		for (final ProposedTag annotatedTag : proposedTags.values()) {
			tagNamesToSearchFor.add(annotatedTag.name());
		}

		final List<String> results = new ArrayList<String>();
		final Collection<Ref> remoteTags = repo.allRemoteTags(remoteUrl);
		for (final Ref remoteTag : remoteTags) {
			for (final String proposedTag : tagNamesToSearchFor) {
				if (remoteTag.getName().equals("refs/tags/" + proposedTag)) {
					results.add(proposedTag);
				}
			}
		}
		return results;
	}

	@Override
	public ProposedTags build() throws SCMException {
		final List<String> matchingRemoteTags = getMatchingRemoteTags();
		if (!matchingRemoteTags.isEmpty()) {
			final SCMException exception = new SCMException(
					"Cannot release because there is already a tag with the same build number on the remote Git repo.");
			for (final String matchingRemoteTag : matchingRemoteTags) {
				exception.add(" * There is already a tag named %s in the remote repo.", matchingRemoteTag);
			}
			throw exception.add("Please try releasing again with a new build number.");
		}
		return new DefaultProposedTags(remoteUrl, proposedTags);
	}

}
