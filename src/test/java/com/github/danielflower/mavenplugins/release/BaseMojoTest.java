package com.github.danielflower.mavenplugins.release;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.junit.Before;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.log.LogHolder;
import com.github.danielflower.mavenplugins.release.reactor.ReactorBuilderFactory;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

/**
 * @author Roland Hauser sourcepond@gmail.com
 *
 */
public class BaseMojoTest {
	private static final String KNOWN_HOSTS = "anyKnownHosts";
	private static final String SERVER_ID = "anyServerId";
	private static final String SETTINGS_IDENTITY_FILE = "settingsIdentityFile";
	private static final String SETTINGS_PASSPHRASE = "settingsPassphrase";
	private static final String POM_IDENTITY_FILE = "pomIdentityFile";
	private static final String POM_PASSPHRASE = "pomPassphrase";
	private final Log log = mock(Log.class);
	private final Settings settings = mock(Settings.class);
	private final Server server = mock(Server.class);
	private final ReactorBuilderFactory reactorBuilderFactory = mock(ReactorBuilderFactory.class);
	private final SCMRepository repository = mock(SCMRepository.class);
	private final LogHolder logHolder = mock(LogHolder.class);
	private BaseMojo mojo;

	@Before
	public void setup() throws ValidationException {
		mojo = new BaseMojo(reactorBuilderFactory, repository, logHolder) {

			@Override
			public void execute() throws MojoExecutionException, MojoFailureException {
				// noop
			}
		};

		when(server.getPrivateKey()).thenReturn(SETTINGS_IDENTITY_FILE);
		when(server.getPassphrase()).thenReturn(SETTINGS_PASSPHRASE);
		when(settings.getServer(SERVER_ID)).thenReturn(server);
		mojo.setSettings(settings);
		mojo.setLog(log);
		JschConfigSessionFactory.setInstance(null);
	}

	@Test
	public void configureJsch_ServerIdDoesNotExist() {
		when(settings.getServer(SERVER_ID)).thenReturn(null);
		mojo.setServerId(SERVER_ID);
		mojo.configureJsch();
		verify(log).warn("No server configuration in Maven settings found with id anyServerId");
	}

	@Test
	public void configureJsch_SshAgentDisabled() {
		mojo.disableSshAgent();
		mojo.configureJsch();
		assertEquals("org.eclipse.jgit.transport.DefaultSshSessionFactory",
				JschConfigSessionFactory.getInstance().getClass().getName());
	}

	private void assertIdentity(final String identityFile, final String passphrase) {
		final SshAgentSessionFactory factory = (SshAgentSessionFactory) JschConfigSessionFactory.getInstance();
		assertEquals(identityFile, factory.getIdentityFile());
		assertEquals(passphrase, factory.getPassphraseOrNull());
	}

	@Test
	public void configureJsch_PomIdentityFile() {
		mojo.setPrivateKey(POM_IDENTITY_FILE);
		mojo.setPassphrase(POM_PASSPHRASE);
		mojo.configureJsch();
		assertIdentity(POM_IDENTITY_FILE, POM_PASSPHRASE);
	}

	@Test
	public void configureJsch_SettingsIdentityFile() {
		mojo.setServerId(SERVER_ID);
		mojo.configureJsch();
		assertIdentity(SETTINGS_IDENTITY_FILE, SETTINGS_PASSPHRASE);
	}

	@Test
	public void configureJsch_CustomIdentityOverridesPom() {
		mojo.setServerId(SERVER_ID);
		mojo.setPrivateKey(POM_IDENTITY_FILE);
		mojo.configureJsch();
		assertIdentity(POM_IDENTITY_FILE, SETTINGS_PASSPHRASE);
	}

	@Test
	public void configureJsch_CustomPassphraseOverridesPom() {
		mojo.setServerId(SERVER_ID);
		mojo.setPassphrase(POM_PASSPHRASE);
		mojo.configureJsch();
		assertIdentity(SETTINGS_IDENTITY_FILE, POM_PASSPHRASE);
	}

	@Test
	public void configureJsch_CustomKnownHosts() {
		mojo.setKnownHosts(KNOWN_HOSTS);
		mojo.configureJsch();
		final SshAgentSessionFactory factory = (SshAgentSessionFactory) JschConfigSessionFactory.getInstance();
		assertEquals(KNOWN_HOSTS, factory.getKnownHostsOrNull());
	}
}
