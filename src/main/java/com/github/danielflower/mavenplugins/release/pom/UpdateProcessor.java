package com.github.danielflower.mavenplugins.release.pom;

import static java.lang.String.format;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.reactor.ReleasableModule;

@Component(role = Updater.class)
final class UpdateProcessor implements Updater {
	static final String DEPENDENCY_ERROR_SUMMARY = "Cannot release with references to snapshot dependencies";
	static final String DEPENDENCY_ERROR_INTRO = "The following dependency errors were found:";

	@Requirement(role = ContextFactory.class)
	private ContextFactory contextFactory;

	@Requirement(role = PomWriterFactory.class)
	private PomWriterFactory writerFactory;

	@Requirement(role = Log.class)
	private Log log;

	@Requirement(role = Command.class)
	private List<Command> commands;

	void setCommands(final List<Command> commands) {
		this.commands = commands;
	}

	void setPomWriterFactory(final PomWriterFactory writerFactory) {
		this.writerFactory = writerFactory;
	}

	void setContextFactory(final ContextFactory contextFactory) {
		this.contextFactory = contextFactory;
	}

	void setLog(final Log log) {
		this.log = log;
	}

	private List<String> process(final MavenProject project, final Context context, final String newVersion) {
		for (final Command cmd : commands) {
			cmd.alterModel(context);
		}
		return context.getErrors();
	}

	@Override
	public ChangeSet updatePoms(final Reactor reactor) throws POMUpdateException {
		final boolean incrementSnapshotVersionAfterRelease = false;
		final PomWriter writer = writerFactory.newWriter();
		final List<String> errors = new LinkedList<String>();

		for (final ReleasableModule module : reactor) {
			final MavenProject project = module.getProject();

			// TODO: If a module will not be released, is further processing
			// necessary or should we continue the loop here?
			if (module.willBeReleased()) {
				log.info(format("Going to release %s %s", module.getArtifactId(),
						module.getVersion().getReleaseVersion()));
			}

			errors.addAll(process(project,
					contextFactory.newReleaseContext(reactor, project, incrementSnapshotVersionAfterRelease),
					module.getVersion().getReleaseVersion()));

			if (incrementSnapshotVersionAfterRelease) {

			}

			// Mark project to be written at a later stage; if an exception
			// occurs, we don't need to revert anything.
			writer.addProject(project);
		}

		if (!errors.isEmpty()) {
			final POMUpdateException exception = new POMUpdateException(DEPENDENCY_ERROR_SUMMARY);
			exception.add(DEPENDENCY_ERROR_INTRO);
			for (final String dependencyError : errors) {
				exception.add(" * %s", dependencyError);
			}
			throw exception;
		}

		// At this point it's guaranteed that no dependency errors occurred.
		return writer.writePoms();
	}
}
