package com.github.danielflower.mavenplugins.release.scm;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;

import java.util.Iterator;
import java.util.Map;

final class DefaultProposedTags implements ProposedTags {
	static final String KEY_FORMAT = "%s/%s/%s";
	private final Map<String, ProposedTag> proposedTags;
	private final String remoteUrl;

	DefaultProposedTags(final String remoteUrl, final Map<String, ProposedTag> proposedTags) {
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
