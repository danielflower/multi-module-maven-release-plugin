package com.github.danielflower.mavenplugins.release.scm;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.lib.Ref;

final class DefaultProposedTags implements ProposedTags {
	static final String KEY_FORMAT = "%s/%s/%s";
	private final Map<String, ProposedTag> proposedTags;

	// TODO: Removed this when getMatchingRemoteTags is removed
	private final GitRepository repo;

	private final String remoteUrl;

	DefaultProposedTags(final GitRepository repo, final String remoteUrl, final Map<String, ProposedTag> proposedTags) {
		this.repo = repo;
		this.remoteUrl = remoteUrl;
		this.proposedTags = proposedTags;
	}

	@Override
	public void tagAndPushRepo() throws SCMException {
		for (final ProposedTag tag : proposedTags.values()) {
			tag.tagAndPush(remoteUrl);
		}
	}

	@Override
	public List<String> getMatchingRemoteTags() throws SCMException {
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
	public ProposedTag getTag(final String tag, final String version, final long buildNumber) throws SCMException {
		final String key = toKey(tag, version, buildNumber);
		final ProposedTag proposedTag = proposedTags.get(key);
		if (proposedTag == null) {
			throw new SCMException("No proposed tag registered %s", key);
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
