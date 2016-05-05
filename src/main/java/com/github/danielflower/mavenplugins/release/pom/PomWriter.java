package com.github.danielflower.mavenplugins.release.pom;

import static java.lang.String.format;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
class PomWriter {
	private final Collection<MavenProject> changedProjects = new LinkedList<>();
	private final SCMRepository repository;
	private final Log log;

	PomWriter(final SCMRepository repository, final Log log) {
		this.repository = repository;
		this.log = log;
	}

	void addProject(final MavenProject project) {
		changedProjects.add(project);
	}

	List<File> writePoms() throws ValidationException {
		final List<File> changedFiles = new LinkedList<>();
		try {
			for (final MavenProject project : changedProjects) {
				final File pom = project.getFile().getCanonicalFile();
				changedFiles.add(pom);
				final Writer fileWriter = new FileWriter(pom);

				final Model originalModel = project.getOriginalModel();
				try {
					final MavenXpp3Writer pomWriter = new MavenXpp3Writer();
					pomWriter.write(fileWriter, originalModel);
				} finally {
					fileWriter.close();
				}
			}
		} catch (final IOException e) {
			try {
				repository.revertChanges(log, changedFiles);
			} catch (final IOException revertException) {
				log.error(format("Reverting changed POMs %s failed!", changedFiles), revertException);
			}
			throw new ValidationException("Unexpected exception while setting the release versions in the pom", e);
		}

		return changedFiles;
	}
}
