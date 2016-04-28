package com.github.danielflower.mavenplugins.release;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.reactor.ReactorBuilder;
import com.github.danielflower.mavenplugins.release.reactor.ReactorBuilderFactory;

/**
 * @author Roland Hauser sourcepond@gmail.com
 *
 */
public abstract class BaseMojo extends AbstractMojo {
	/**
	 * The Maven Project.
	 */
	@Parameter(property = "project", required = true, readonly = true, defaultValue = "${project}")
	protected MavenProject project;

	@Parameter(property = "projects", required = true, readonly = true, defaultValue = "${reactorProjects}")
	protected List<MavenProject> projects;

	/**
	 * <p>
	 * The build number to use in the release version. Given a snapshot version
	 * of "1.0-SNAPSHOT" and a buildNumber value of "2", the actual released
	 * version will be "1.0.2".
	 * </p>
	 * <p>
	 * By default, the plugin will automatically find a suitable build number.
	 * It will start at version 0 and increment this with each release.
	 * </p>
	 * <p>
	 * This can be specified using a command line parameter ("-DbuildNumber=2")
	 * or in this plugin's configuration.
	 * </p>
	 */
	@Parameter(property = "buildNumber")
	protected Long buildNumber;

	/**
	 * The modules to release, or no value to to release the project from the
	 * root pom, which is the default. The selected module plus any other
	 * modules it needs will be built and released also. When run from the
	 * command line, this can be a comma-separated list of module names.
	 */
	@Parameter(alias = "modulesToRelease", property = "modulesToRelease")
	protected List<String> modulesToRelease;

	/**
	 * A module to force release on, even if no changes has been detected.
	 */
	@Parameter(alias = "forceRelease", property = "forceRelease")
	protected List<String> modulesToForceRelease;

	@Parameter(property = "disableSshAgent")
	private boolean disableSshAgent;

	/**
	 * Specifies whether the release build should run with the "-X" switch.
	 */
	@Parameter(property = "debugEnabled")
	protected boolean debugEnabled;

	/**
	 * Specifies whether the release build should run with the "-e" switch.
	 */
	@Parameter(property = "stacktraceEnabled")
	protected boolean stacktraceEnabled;

	@Parameter(defaultValue = "${settings}", readonly = true, required = true)
	private Settings settings;

	/**
	 * If set, the identityFile and passphrase will be read from the Maven
	 * settings file.
	 */
	@Parameter(property = "serverId")
	private String serverId;

	/**
	 * If set, this file will be used to specify the known_hosts. This will
	 * override any default value.
	 */
	@Parameter(property = "knownHosts")
	private String knownHosts;

	/**
	 * Specifies the private key to be used.
	 */
	@Parameter(property = "privateKey")
	private String privateKey;

	/**
	 * Specifies the passphrase to be used with the identityFile specified.
	 */
	@Parameter(property = "passphrase")
	private String passphrase;

	private final ReactorBuilderFactory builderFactory;

	protected BaseMojo(final ReactorBuilderFactory builderFactory) {
		this.builderFactory = builderFactory;
	}

	final void setSettings(final Settings settings) {
		this.settings = settings;
	}

	final void setServerId(final String serverId) {
		this.serverId = serverId;
	}

	final void setKnownHosts(final String knownHosts) {
		this.knownHosts = knownHosts;
	}

	final void setPrivateKey(final String privateKey) {
		this.privateKey = privateKey;
	}

	final void setPassphrase(final String passphrase) {
		this.passphrase = passphrase;
	}

	final void disableSshAgent() {
		disableSshAgent = true;
	}

	protected List<AnnotatedTag> figureOutTagNamesAndThrowIfAlreadyExists(final Reactor reactor)
			throws GitAPIException, ValidationException {
		final List<AnnotatedTag> tags = new ArrayList<AnnotatedTag>();
		for (final ReleasableModule module : reactor) {
			if (!module.willBeReleased()) {
				continue;
			}
			if (modulesToRelease == null || modulesToRelease.size() == 0 || module.isOneOf(modulesToRelease)) {
				final String tag = module.getTagName();
				if (reactor.getLocalRepo().hasLocalTag(tag)) {
					final String summary = "There is already a tag named " + tag + " in this repository.";
					throw new ValidationException(summary,
							asList(summary, "It is likely that this version has been released before.",
									"Please try incrementing the build number and trying again."));
				}

				final AnnotatedTag annotatedTag = AnnotatedTag.create(tag, module.getVersion(),
						module.getBuildNumber());
				tags.add(annotatedTag);
			}
		}
		final List<String> matchingRemoteTags = reactor.getLocalRepo().remoteTagsFrom(tags);
		if (matchingRemoteTags.size() > 0) {
			final String summary = "Cannot release because there is already a tag with the same build number on the remote Git repo.";
			final List<String> messages = new ArrayList<String>();
			messages.add(summary);
			for (final String matchingRemoteTag : matchingRemoteTags) {
				messages.add(" * There is already a tag named " + matchingRemoteTag + " in the remote repo.");
			}
			messages.add("Please try releasing again with a new build number.");
			throw new ValidationException(summary, messages);
		}
		return tags;
	}

	private static String getRemoteUrlOrNullIfNoneSet(final Scm scm) throws ValidationException {
		if (scm == null) {
			return null;
		}
		String remote = scm.getDeveloperConnection();
		if (remote == null) {
			remote = scm.getConnection();
		}
		if (remote == null) {
			return null;
		}
		return GitHelper.scmUrlToRemote(remote);
	}

	protected void printBigErrorMessageAndThrow(final String terseMessage, final List<String> linesToLog)
			throws MojoExecutionException {
		final Log log = getLog();
		log.error("");
		log.error("");
		log.error("");
		log.error("************************************");
		log.error("Could not execute the release plugin");
		log.error("************************************");
		log.error("");
		log.error("");
		for (final String line : linesToLog) {
			log.error(line);
		}
		log.error("");
		log.error("");
		throw new MojoExecutionException(terseMessage);
	}

	protected final Reactor newReactor() throws ValidationException, MojoExecutionException, GitAPIException {
		final ReactorBuilder builder = builderFactory.newBuilder();
		return builder.setLog(getLog())
				.setGitRepo(LocalGitRepo.fromCurrentDir(getRemoteUrlOrNullIfNoneSet(project.getScm())))
				.setRootProject(project).setProjects(projects).setBuildNumber(buildNumber)
				.setModulesToForceRelease(modulesToForceRelease).build();
	}

	protected final void configureJsch(final Log log) {
		if (!disableSshAgent) {
			if (serverId != null) {
				final Server server = settings.getServer(serverId);
				if (server != null) {
					privateKey = privateKey == null ? server.getPrivateKey() : privateKey;
					passphrase = passphrase == null ? server.getPassphrase() : passphrase;
				} else {
					log.warn(format("No server configuration in Maven settings found with id %s", serverId));
				}
			}

			JschConfigSessionFactory.setInstance(new SshAgentSessionFactory(log, knownHosts, privateKey, passphrase));
		}
	}
}
