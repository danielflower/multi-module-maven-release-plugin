package com.github.danielflower.mavenplugins.release.pom;

import java.io.IOException;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;

public interface Updater {

	/**
	 * Updates all necessary POMs and returns the changed files.
	 * 
	 * @param reactor
	 *            Reactor instance, must not be {@code null}
	 * @return List of updated POM files.
	 * @throws IOException
	 * @throws POMUpdateException
	 */
	ChangeSet updatePoms(Reactor reactor, String remoteUrl, boolean incrementSnapshotVersionAfterRelease)
			throws POMUpdateException;

}
