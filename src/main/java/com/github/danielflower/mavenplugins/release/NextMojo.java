package com.github.danielflower.mavenplugins.release;

import static java.lang.String.format;

import java.util.List;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.eclipse.jgit.transport.JschConfigSessionFactory;

import com.github.danielflower.mavenplugins.release.log.LogHolder;
import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.reactor.ReactorBuilder;
import com.github.danielflower.mavenplugins.release.reactor.ReactorBuilderFactory;
import com.github.danielflower.mavenplugins.release.reactor.ReactorException;
import com.github.danielflower.mavenplugins.release.reactor.ReleasableModule;
import com.github.danielflower.mavenplugins.release.scm.ProposedTags;
import com.github.danielflower.mavenplugins.release.scm.ProposedTagsBuilder;
import com.github.danielflower.mavenplugins.release.scm.SCMException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

/**
 * Logs the versions of the modules that the releaser will release on the next
 * release. Does not run the build nor tag the repo.
 * 
 * @since 1.4.0
 */
@Mojo(name = "next", requiresDirectInvocation = true, // this should not be
														// bound to a phase as
														// this plugin starts a
														// phase itself
inheritByDefault = true, // so you can configure this in a shared parent pom
requiresProject = true, // this can only run against a maven project
aggregator = true // the plugin should only run once against the aggregator pom
)
public class NextMojo extends AbstractMojo {
	static final String ERROR_SUMMARY = "Cannot run the release plugin with a non-Git version control system";
	static final String GIT_PREFIX = "scm:git:";

	/**
	 * The Maven Project.
	 */
	@Parameter(property = "project", required = true, readonly = true, defaultValue = "${project}")
	protected MavenProject project;

	@Parameter(property = "projects", required = true, readonly = true, defaultValue = "${reactorProjects}")
	protected List<MavenProject> projects;

	/**
	 * <p>
	 * Tells the plugin to use the last digit of the current version of the
	 * POM(s) to be released as build number. Given a snapshot version of
	 * "1.0.3-SNAPSHOT", the actual released version will be "1.0.3".
	 * </p>
	 * 
	 * <p>
	 * This setting <em>cannot</em> be used in conjunction with
	 * {@link #buildNumber}. If this property is set to {@code true} and
	 * {@link #buildNumber} has non-{@code null} value, a
	 * {@link MojoExecutionException} will be caused to be thrown.
	 * </p>
	 */
	@Parameter(property = "useLastDigitAsBuildNumber")
	protected boolean useLastDigitAsBuildNumber;

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

	@Component
	private ReactorBuilderFactory builderFactory;

	@Component
	protected SCMRepository repository;

	@Component
	private LogHolder logHolder;

	final void setRepository(final SCMRepository repository) {
		this.repository = repository;
	}

	final void setReactorBuilderFactory(final ReactorBuilderFactory builderFactory) {
		this.builderFactory = builderFactory;
	}

	final void setLogHolder(final LogHolder logHolder) {
		this.logHolder = logHolder;
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

	static String getRemoteUrlOrNullIfNoneSet(final Scm scm) throws PluginException {
		String remote = null;
		if (scm != null) {
			remote = scm.getDeveloperConnection();
			if (remote == null) {
				remote = scm.getConnection();
			}
			if (remote != null) {
				if (!remote.startsWith(GIT_PREFIX)) {
					throw new PluginException(ERROR_SUMMARY).add("The value in your scm tag is %s", remote);
				}
				remote = remote.substring(GIT_PREFIX.length()).replace("file://localhost/", "file:///");
			}
		}
		return remote;
	}

	protected ProposedTags figureOutTagNamesAndThrowIfAlreadyExists(final Reactor reactor, final String remoteUrl)
			throws ReactorException, SCMException {
		final ProposedTagsBuilder builder = repository.newProposedTagsBuilder(remoteUrl);
		for (final ReleasableModule module : reactor) {
			if (!module.willBeReleased()) {
				continue;
			}
			if (modulesToRelease == null || modulesToRelease.size() == 0 || module.isOneOf(modulesToRelease)) {
				builder.add(module.getTagName(), module.getVersion(), module.getBuildNumber());
			}
		}
		return builder.build();
	}

	protected final Reactor newReactor(final String remoteUrl) throws ReactorException {
		final ReactorBuilder builder = builderFactory.newBuilder();
		return builder.setRootProject(project).setProjects(projects)
				.setUseLastDigitAsBuildNumber(useLastDigitAsBuildNumber).setBuildNumber(buildNumber)
				.setModulesToForceRelease(modulesToForceRelease).setRemoteUrl(remoteUrl).build();
	}

	protected final void configureJsch() {
		if (!disableSshAgent) {
			if (serverId != null) {
				final Server server = settings.getServer(serverId);
				if (server != null) {
					privateKey = privateKey == null ? server.getPrivateKey() : privateKey;
					passphrase = passphrase == null ? server.getPassphrase() : passphrase;
				} else {
					getLog().warn(format("No server configuration in Maven settings found with id %s", serverId));
				}
			}

			JschConfigSessionFactory
					.setInstance(new SshAgentSessionFactory(getLog(), knownHosts, privateKey, passphrase));
		}
	}

	@Override
	public final void setLog(final Log log) {
		super.setLog(log);
		logHolder.setLog(log);
	}

	protected void execute(final Reactor reactor, final ProposedTags proposedTags)
			throws MojoExecutionException, PluginException {
		// noop by default
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (useLastDigitAsBuildNumber && buildNumber != null) {
			throw new MojoExecutionException(
					"You cannot use 'useLastDigitAsBuildNumber' in conjunction with 'buildNumber'!");
		}
		try {
			repository.errorIfNotClean();
			configureJsch();
			final String remoteUrl = getRemoteUrlOrNullIfNoneSet(project.getScm());
			final Reactor reactor = newReactor(remoteUrl);
			execute(reactor, figureOutTagNamesAndThrowIfAlreadyExists(reactor, remoteUrl));
		} catch (final PluginException e) {
			e.printBigErrorMessageAndThrow(getLog());
		}
	}
}
