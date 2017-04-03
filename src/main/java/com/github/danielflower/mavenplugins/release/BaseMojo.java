package com.github.danielflower.mavenplugins.release;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;

import com.github.danielflower.mavenplugins.release.repository.LocalGitRepo;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableModuleVersion;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableQualifiedArtifact;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseDateSingleton;

/**
 * @author Roland Hauser sourcepond@gmail.com
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

    /**
     * Determines the action to take when no module changes are detected. Possible values:
     * {@code ReleaseAll}, {@code ReleaseNone}, {@code FailBuild}
     */
    @Parameter(alias = "noChangesAction", defaultValue = "ReleaseAll", property = "noChangesAction")
    protected NoChangesAction noChangesAction;
    @Parameter(defaultValue = "false", alias = "performBugfixRelease", property = "performBugfixRelease")
    protected boolean bugfixRelease;
    @Parameter(property = "disableSshAgent")
    private boolean disableSshAgent;
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

    static void printBigErrorMessageAndThrow(Log log, String terseMessage, List<String> linesToLog) throws
                                                                                                    MojoExecutionException {
        log.error("");
        log.error("");
        log.error("");
        log.error("************************************");
        log.error("Could not execute the release plugin");
        log.error("************************************");
        log.error("");
        log.error("");
        for (String line : linesToLog) {
            log.error(line);
        }
        log.error("");
        log.error("");
        throw new MojoExecutionException(terseMessage);
    }

    final Settings getSettings() {
        return settings;
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

    protected final void configureJsch(final Log log) {
        if (!disableSshAgent) {
            if (serverId != null) {
                final Server server = settings.getServer(serverId);
                if (server != null) {
                    privateKey = privateKey == null
                                 ? server.getPrivateKey()
                                 : privateKey;
                    passphrase = passphrase == null
                                 ? server.getPassphrase()
                                 : passphrase;
                } else {
                    log.warn(format("No server configuration in Maven settings found with id %s", serverId));
                }
            }

            JschConfigSessionFactory.setInstance(new SshAgentSessionFactory(log, knownHosts, privateKey, passphrase));
        }
    }

    List<ImmutableModuleVersion> figureOutTagNamesAndThrowIfAlreadyExists(List<ReleasableModule> modules,
                                                                          LocalGitRepo git) throws
                                                                                                         GitAPIException,
                                                                                                         ValidationException {
        List<ImmutableModuleVersion> tags = new ArrayList<>();
        for (ReleasableModule module : modules) {
            /*
            if (!module.()) {
                // TODO add version anyway
                continue;
            }
            */
            if (modulesToRelease == null || modulesToRelease.size() == 0 || modulesToRelease.contains(
                module.getProject().getArtifactId())) {
                final ImmutableModuleVersion.Builder builder = ImmutableModuleVersion.builder();
                builder.version(module.getVersion());
                builder.releaseTag(tagName());
                final ImmutableQualifiedArtifact.Builder artifactBuilder = ImmutableQualifiedArtifact.builder();
                artifactBuilder.groupId(module.getProject().getGroupId());
                artifactBuilder.artifactId(module.getProject().getArtifactId());
                builder.artifact(artifactBuilder.build());
                builder.releaseDate(ReleaseDateSingleton.getInstance().getDate());
                tags.add(builder.build());
            }
        }
        // TODO check for single tag.
        return tags;
    }

    protected String tagName() {
        final ReleaseDateSingleton releaseDate = ReleaseDateSingleton.getInstance();
        return project.getArtifactId() + "-" + releaseDate.asFileSuffix();
    }
}
