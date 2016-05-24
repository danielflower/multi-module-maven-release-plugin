package com.github.danielflower.mavenplugins.release.pom;

import java.io.File;

/**
 * Represents a set of changed files. This interface extends
 * {@link AutoCloseable}; when {@link #close()} is called all changed files will
 * be reverted.
 * 
 *
 */
public interface ChangeSet extends Iterable<File>, AutoCloseable {

	/**
	 * Sets the exception to be thrown when {@link #close()} is called. If an
	 * exception is set and the revert of the changed files fails, the revert
	 * exception will only be logged. This is to keep the original exception so
	 * the root cause isn't lost. If no exception is set and the revert
	 * operation fails, the revert exception will be caused to be thrown because
	 * that is the root problem
	 * 
	 * @param e
	 *            Exception which shall be caused to be thrown when
	 *            {@link #close()} is called.
	 */
	void setFailure(Exception e);

	@Override
	void close() throws ChangeSetCloseException;
}
