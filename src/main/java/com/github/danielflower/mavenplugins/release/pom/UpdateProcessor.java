package com.github.danielflower.mavenplugins.release.pom;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

@Named
@Singleton
class UpdateProcessor implements Updater {
	private final SCMRepository repository;
	private final List<Command> commands;

	@Inject
	public UpdateProcessor(final SCMRepository repository, final List<Command> commands) {
		this.repository = repository;
		this.commands = commands;
	}

	private List<String> process(final Log log, final Reactor reactor, final MavenProject project,
			final String newVersion) {
		final DefaultContext context = new DefaultContext(log, reactor, project, newVersion);
		final Model originalModel = project.getOriginalModel();
		originalModel.setVersion(newVersion);

		for (final Command cmd : commands) {
			cmd.alterModel(context);
		}

		return context.getErrors();
	}

	private UpdateResult execute(final Log log, final Reactor reactor) {
		final List<File> changedPoms = new ArrayList<File>();
		final List<String> errors = new ArrayList<String>();
		for (final ReleasableModule module : reactor) {
			try {
				final MavenProject project = module.getProject();
				if (module.willBeReleased()) {
					log.info("Going to release " + module.getArtifactId() + " " + module.getNewVersion());
				}

				final List<String> errorsForCurrentPom = process(log, reactor, project, module.getNewVersion());
				errors.addAll(errorsForCurrentPom);

				final File pom = project.getFile().getCanonicalFile();
				changedPoms.add(pom);
				final Writer fileWriter = new FileWriter(pom);

				final Model originalModel = project.getOriginalModel();
				try {
					final MavenXpp3Writer pomWriter = new MavenXpp3Writer();
					pomWriter.write(fileWriter, originalModel);
				} finally {
					fileWriter.close();
				}
			} catch (final Exception e) {
				return new UpdateResult(changedPoms, errors, e);
			}
		}
		return new UpdateResult(changedPoms, errors, null);
	}

	@Override
	public List<File> updatePoms(final Log log, final Reactor reactor) throws IOException {
		final UpdateResult result = execute(log, reactor);
		if (!result.success()) {
			log.info("Going to revert changes because there was an error.");
			repository.revertChanges(log, result.alteredPoms);
			if (result.unexpectedException != null) {
				throw new ValidationException("Unexpected exception while setting the release versions in the pom",
						result.unexpectedException);
			} else {
				final String summary = "Cannot release with references to snapshot dependencies";
				final List<String> messages = new ArrayList<String>();
				messages.add(summary);
				messages.add("The following dependency errors were found:");
				for (final String dependencyError : result.dependencyErrors) {
					messages.add(" * " + dependencyError);
				}
				throw new ValidationException(summary, messages);
			}
		}
		return result.alteredPoms;
	}
}
