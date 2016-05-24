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
	private Exception failure;

	DefaultChangeSet(final Log log, final SCMRepository repository) {
		this.log = log;
		this.repository = repository;
	}

	@Override
	public void close() throws ChangeSetCloseException {
		try {
			if (!repository.revertChanges(this)) {
				if (failure == null) {
					throw new ChangeSetCloseException(REVERT_ERROR_MESSAGE);
				} else {
					log.warn(REVERT_ERROR_MESSAGE);
				}
			}
		} catch (final SCMException e) {
			throw new ChangeSetCloseException(e, REVERT_ERROR_MESSAGE);
		}
		if (failure != null) {
			log.info("Reverted changes because there was an error.");
			throw new ChangeSetCloseException(failure, REVERT_ERROR_MESSAGE);
		}
	}

	@Override
	public void setFailure(final Exception e) {
		failure = e;
	}
}
