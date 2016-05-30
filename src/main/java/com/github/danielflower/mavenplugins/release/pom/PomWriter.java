package com.github.danielflower.mavenplugins.release.pom;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

/**
 * @author rolandhauser
 *
 */
@Component(role = PomWriter.class)
class PomWriter {
	static final String EXCEPTION_MESSAGE = "Unexpected exception while setting the release versions in the pom";
	private final Collection<MavenProject> releases = new LinkedList<>();
	private final Collection<MavenProject> snapshotVersionIncrements = new LinkedList<>();

	@Requirement(role = SCMRepository.class)
	private SCMRepository repository;

	@Requirement(role = MavenXpp3Writer.class)
	private MavenXpp3Writer writer;

	@Requirement(role = Log.class)
	private Log log;

	void setRepository(final SCMRepository repository) {
		this.repository = repository;
	}

	void setWriter(final MavenXpp3Writer writer) {
		this.writer = writer;
	}

	void setLog(final Log log) {
		this.log = log;
	}

	void markRelease(final MavenProject project) {
		releases.add(project);
	}

	void markSnapshotVersionIncrement(final MavenProject project) {
		snapshotVersionIncrements.add(project);
	}

	ChangeSet writePoms(final String remoteUrl) throws POMUpdateException {
		final SnapshotIncrementChangeSet snapshotIncrementChangeSet = new SnapshotIncrementChangeSet(log, repository,
				writer, remoteUrl);
		snapshotIncrementChangeSet.addAll(this.snapshotVersionIncrements);
		final DefaultChangeSet changedFiles = new DefaultChangeSet(log, repository, snapshotIncrementChangeSet);
		try {
			for (final MavenProject project : releases) {
				// It's necessary to use the canonical file here, otherwise GIT
				// revert can fail when symbolic links are used (ends up in an
				// empty path and revert fails).
				final File changedFile = project.getFile().getCanonicalFile();
				changedFiles.add(changedFile);
				try (final Writer fileWriter = new FileWriter(changedFile)) {
					writer.write(fileWriter, project.getOriginalModel());
				}
			}
		} catch (final IOException e) {
			changedFiles.setFailure(EXCEPTION_MESSAGE, e);
			changedFiles.close();
		}

		return changedFiles;
	}
}
