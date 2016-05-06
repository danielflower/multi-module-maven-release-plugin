package com.github.danielflower.mavenplugins.release.scm;

import static com.github.danielflower.mavenplugins.release.scm.DefaultProposedTag.BUILD_NUMBER;
import static com.github.danielflower.mavenplugins.release.scm.DefaultProposedTag.VERSION;
import static com.github.danielflower.mavenplugins.release.scm.DefaultProposedTags.toKey;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.json.simple.JSONObject;

import com.github.danielflower.mavenplugins.release.ValidationException;

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
	public ProposedTagsBuilder add(final String tag, final String version, final long buildNumber)
			throws ValidationException, GitAPIException {
		if (repo.hasLocalTag(tag)) {
			final String summary = "There is already a tag named " + tag + " in this repository.";
			throw new ValidationException(summary,
					asList(summary, "It is likely that this version has been released before.",
							"Please try incrementing the build number and trying again."));
		}
		final JSONObject message = new JSONObject();
		message.put(VERSION, version);
		message.put(BUILD_NUMBER, String.valueOf(buildNumber));
		final ProposedTag proposedTag = new DefaultProposedTag(git, null, tag, message);
		proposedTags.put(toKey(tag, version, buildNumber), new DefaultProposedTag(git, null, tag, message));
		return this;
	}

	private List<String> getMatchingRemoteTags() throws GitAPIException {
		final List<String> tagNamesToSearchFor = new ArrayList<String>();
		for (final ProposedTag annotatedTag : proposedTags.values()) {
			tagNamesToSearchFor.add(annotatedTag.name());
		}

		final List<String> results = new ArrayList<String>();
		final Collection<Ref> remoteTags = repo.allRemoteTags();
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
	public ProposedTags build() throws ValidationException, GitAPIException {
		final List<String> matchingRemoteTags = getMatchingRemoteTags();
		if (!matchingRemoteTags.isEmpty()) {
			final String summary = "Cannot release because there is already a tag with the same build number on the remote Git repo.";
			final List<String> messages = new ArrayList<String>();
			messages.add(summary);
			for (final String matchingRemoteTag : matchingRemoteTags) {
				messages.add(" * There is already a tag named " + matchingRemoteTag + " in the remote repo.");
			}
			messages.add("Please try releasing again with a new build number.");
			throw new ValidationException(summary, messages);
		}
		return new DefaultProposedTags(log, git, repo, remoteUrl, proposedTags);
	}

}
