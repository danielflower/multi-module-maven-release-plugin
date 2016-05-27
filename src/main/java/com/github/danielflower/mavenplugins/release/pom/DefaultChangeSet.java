package com.github.danielflower.mavenplugins.release.pom;

import java.io.File;
import java.util.LinkedList;

import org.apache.maven.plugin.logging.Log;

import com.github.danielflower.mavenplugins.release.scm.SCMException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

/**
 * Default implementation of the {@link ChangeSet} interface.
 *
 */
@SuppressWarnings("serial")
final class DefaultChangeSet extends LinkedList<File>implements ChangeSet {
	static final String REVERT_ERROR_MESSAGE = "Could not revert changes - working directory is no longer clean. Please revert changes manually";
	private final Log log;
	private final SCMRepository repository;
	private final SnapshotIncrementChangeSet snapshotIncrementChangeSet;
	private ChangeSetCloseException failure;

	DefaultChangeSet(final Log log, final SCMRepository repository,
			final SnapshotIncrementChangeSet snapshotIncrementChangeSet) {
		this.log = log;
		this.repository = repository;
		this.snapshotIncrementChangeSet = snapshotIncrementChangeSet;
	}

	@Override
	public void close() throws ChangeSetCloseException {
		try {
			repository.revertChanges(this);
		} catch (final SCMException e) {
			if (failure == null) {
				// throw if you can't revert as that is the root problem
				throw new ChangeSetCloseException(e, REVERT_ERROR_MESSAGE);
			} else {
				// warn if you can't revert but keep throwing the original
				// exception so the root cause isn't lost
				log.warn(REVERT_ERROR_MESSAGE, e);
			}
		}
		if (failure != null) {
			log.info("Reverted changes because there was an error.");
			throw failure;
		}

		snapshotIncrementChangeSet.close();
	}

	@Override
	public void setFailure(final String message, final Exception failure) {
		this.failure = new ChangeSetCloseException(failure, message);
	}
}
