package com.github.danielflower.mavenplugins.release.scm;

import static com.github.danielflower.mavenplugins.release.scm.DefaultProposedTag.BUILD_NUMBER;
import static com.github.danielflower.mavenplugins.release.scm.DefaultProposedTag.VERSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.json.simple.JSONObject;

final class DefaultProposedTags implements ProposedTags {
	private final List<ProposedTag> proposedTags = new LinkedList<>();
	private final Log log;
	private final Git git;
	private final GitRepository repo;
	private final String remoteUrl;

	DefaultProposedTags(final Log log, final Git git, final GitRepository repo, final String remoteUrl) {
		this.log = log;
		this.git = git;
		this.repo = repo;
		this.remoteUrl = remoteUrl;
	}

	@Override
	public ProposedTag add(final String tag, final String version, final long buildNumber) {
		final JSONObject message = new JSONObject();
		message.put(VERSION, version);
		message.put(BUILD_NUMBER, String.valueOf(buildNumber));
		final DefaultProposedTag proposedTag = new DefaultProposedTag(git, null, tag, message);
		proposedTags.add(proposedTag);
		return proposedTag;
	}

	@Override
	public List<String> getMatchingRemoteTags() throws GitAPIException {
		final List<String> tagNamesToSearchFor = new ArrayList<String>();
		for (final ProposedTag annotatedTag : proposedTags) {
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
	public void tagAndPushRepo() throws GitAPIException {
		for (final ProposedTag tag : proposedTags) {
			log.info("About to tag the repository with " + tag.name());
			final Ref tagRef = tag.saveAtHEAD();
			final PushCommand pushCommand = git.push().add(tagRef);
			if (remoteUrl != null) {
				pushCommand.setRemote(remoteUrl);
			}
			pushCommand.call();
		}
	}

}
