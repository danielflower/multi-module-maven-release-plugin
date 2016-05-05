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
	static final String EXCEPTION_MESSAGE = "Unexpected exception while setting the release versions in the pom";
	private final Collection<MavenProject> changedProjects = new LinkedList<>();
	private final SCMRepository repository;
	private final MavenXpp3Writer writer;
	private final Log log;

	PomWriter(final SCMRepository repository, final MavenXpp3Writer writer, final Log log) {
		this.repository = repository;
		this.writer = writer;
		this.log = log;
	}

	void addProject(final MavenProject project) {
		changedProjects.add(project);
	}

	List<File> writePoms() throws ValidationException {
		final List<File> changedFiles = new LinkedList<>();
		try {
			for (final MavenProject project : changedProjects) {
				changedFiles.add(project.getFile());
				try (final Writer fileWriter = new FileWriter(project.getFile())) {
					writer.write(fileWriter, project.getOriginalModel());
				}
			}
		} catch (final IOException e) {
			try {
				repository.revertChanges(log, changedFiles);
			} catch (final IOException revertException) {
				log.error(format("Reverting changed POMs %s failed!", changedFiles), revertException);
			}
			throw new ValidationException(EXCEPTION_MESSAGE, e);
		}

		return changedFiles;
	}
}
