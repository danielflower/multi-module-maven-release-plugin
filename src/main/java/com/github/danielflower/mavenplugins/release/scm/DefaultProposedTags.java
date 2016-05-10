package com.github.danielflower.mavenplugins.release.scm;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import com.github.danielflower.mavenplugins.release.ValidationException;

final class DefaultProposedTags implements ProposedTags {
	static final String KEY_FORMAT = "%s/%s/%s";
	private final Map<String, ProposedTag> proposedTags;
	private final Log log;
	private final Git git;

	// TODO: Removed this when getMatchingRemoteTags is removed
	private final GitRepository repo;

	private final String remoteUrl;

	DefaultProposedTags(final Log log, final Git git, final GitRepository repo, final String remoteUrl,
			final Map<String, ProposedTag> proposedTags) {
		this.log = log;
		this.git = git;
		this.repo = repo;
		this.remoteUrl = remoteUrl;
		this.proposedTags = proposedTags;
	}

	@Override
	public void tagAndPushRepo() throws GitAPIException {
		for (final ProposedTag tag : proposedTags.values()) {
			log.info("About to tag the repository with " + tag.name());
			final Ref tagRef = tag.saveAtHEAD();
			final PushCommand pushCommand = git.push().add(tagRef);
			if (remoteUrl != null) {
				pushCommand.setRemote(remoteUrl);
			}
			pushCommand.call();
		}
	}

	@Override
	public List<String> getMatchingRemoteTags() throws GitAPIException, ValidationException {
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
	public ProposedTag getTag(final String tag, final String version, final long buildNumber)
			throws ValidationException {
		final String key = toKey(tag, version, buildNumber);
		final ProposedTag proposedTag = proposedTags.get(key);
		if (proposedTag == null) {
			throw new ValidationException(format("No proposed tag registered %s", key),
					Collections.<String> emptyList());
		}
		return proposedTag;
	}

	static String toKey(final String tag, final String version, final long buildNumber) {
		return format(KEY_FORMAT, tag, version, buildNumber);
	}

	@Override
	public Iterator<ProposedTag> iterator() {
		return unmodifiableCollection(proposedTags.values()).iterator();
	}
}
