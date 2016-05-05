package com.github.danielflower.mavenplugins.release.pom;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.reactor.Reactor;

@Named
@Singleton
final class UpdateProcessor implements Updater {
	static final String DEPENDENCY_ERROR_SUMMARY = "Cannot release with references to snapshot dependencies";
	static final String DEPENDENCY_ERROR_INTRO = "The following dependency errors were found:";
	private final ContextFactory contextFactory;
	private final PomWriterFactory writerFactory;
	private final List<Command> commands;

	@Inject
	public UpdateProcessor(final ContextFactory contextFactory, final PomWriterFactory writerFactory,
			final List<Command> commands) {
		this.contextFactory = contextFactory;
		this.writerFactory = writerFactory;
		this.commands = commands;
	}

	private List<String> process(final Log log, final Reactor reactor, final MavenProject project,
			final String newVersion) {
		final Context context = contextFactory.newContext(log, reactor, project, newVersion);
		final Model originalModel = project.getOriginalModel();
		originalModel.setVersion(newVersion);

		for (final Command cmd : commands) {
			cmd.alterModel(context);
		}

		return context.getErrors();
	}

	@Override
	public List<File> updatePoms(final Log log, final Reactor reactor) throws IOException, ValidationException {
		final PomWriter writer = writerFactory.newWriter(log);
		final List<String> errors = new ArrayList<String>();

		for (final ReleasableModule module : reactor) {
			final MavenProject project = module.getProject();

			// TODO: If a module will not be released, is further processing
			// necessary or should we continue the loop here?
			if (module.willBeReleased()) {
				log.info(format("Going to release %s %s", module.getArtifactId(), module.getNewVersion()));
			}

			errors.addAll(process(log, reactor, project, module.getNewVersion()));

			// Mark project to be written at a later stage; if an exception
			// occurs, we don't need to revert anything.
			writer.addProject(project);
		}

		if (!errors.isEmpty()) {
			final List<String> messages = new ArrayList<String>();
			messages.add(DEPENDENCY_ERROR_SUMMARY);
			messages.add(DEPENDENCY_ERROR_INTRO);
			for (final String dependencyError : errors) {
				messages.add(format(" * %s", dependencyError));
			}
			throw new ValidationException(DEPENDENCY_ERROR_SUMMARY, messages);
		}

		// At this point it's guaranteed that no dependency errors occurred.
		return writer.writePoms();
	}
}
