package com.github.danielflower.mavenplugins.release.pom;

import static com.github.danielflower.mavenplugins.release.pom.DefaultChangeSet.REVERT_ERROR_MESSAGE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.scm.SCMException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

@SuppressWarnings("serial")
class SnapshotIncrementChangeSet extends LinkedList<MavenProject>implements AutoCloseable {
	static final String IO_EXCEPTION_FORMAT = "Updated pom %s could not be written!";
	static final String PUSH_EXCEPTION_FORMAT = "Changed files %s could not be pushed";
	private final Log log;
	private final SCMRepository repository;
	private final MavenXpp3Writer pomWriter;
	private final String remoteUrl;

	SnapshotIncrementChangeSet(final Log log, final SCMRepository repository, final MavenXpp3Writer pomWriter,
			final String remoteUrl) {
		this.log = log;
		this.repository = repository;
		this.pomWriter = pomWriter;
		this.remoteUrl = remoteUrl;
	}

	@Override
	public void close() throws ChangeSetCloseException {
		final List<File> changedFiles = new LinkedList<>();

		for (final MavenProject project : this) {
			try {
				// It's necessary to use the canonical file here, otherwise GIT
				// revert can fail when symbolic links are used (ends up in an
				// empty path and revert fails).
				final File changedFile = project.getFile().getCanonicalFile();
				changedFiles.add(changedFile);
				try (final Writer fileWriter = new FileWriter(changedFile)) {
					pomWriter.write(fileWriter, project.getOriginalModel());
				}
			} catch (final IOException e) {
				try {
					repository.revertChanges(changedFiles);
				} catch (final SCMException revertException) {
					// warn if you can't revert but keep throwing the original
					// exception so the root cause isn't lost
					log.warn(REVERT_ERROR_MESSAGE, e);
				}
				throw new ChangeSetCloseException(e, IO_EXCEPTION_FORMAT, project);
			}
		}

		try {
			repository.pushChanges(remoteUrl);
		} catch (final SCMException e) {
			throw new ChangeSetCloseException(e, IO_EXCEPTION_FORMAT, changedFiles);
		}
	}
}
