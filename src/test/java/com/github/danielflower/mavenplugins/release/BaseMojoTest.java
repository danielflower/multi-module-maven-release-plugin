package com.github.danielflower.mavenplugins.release;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.junit.Before;
import org.junit.Test;

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
	private final BaseMojo mojo = mock(BaseMojo.class);

	@Before
	public void setup() {
		when(server.getPrivateKey()).thenReturn(SETTINGS_IDENTITY_FILE);
		when(server.getPassphrase()).thenReturn(SETTINGS_PASSPHRASE);
		when(settings.getServer(SERVER_ID)).thenReturn(server);
		mojo.setSettings(settings);
		JschConfigSessionFactory.setInstance(null);
	}

	@Test
	public void configureJsch_ServerIdDoesNotExist() {
		when(settings.getServer(SERVER_ID)).thenReturn(null);
		mojo.setServerId(SERVER_ID);
		mojo.configureJsch(log);
		verify(log).warn("No server configuration in Maven settings found with id anyServerId");
	}
	
	@Test
	public void configureJsch_SshAgentDisabled() {
		mojo.disableSshAgent();
		mojo.configureJsch(log);
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
		mojo.configureJsch(log);
		assertIdentity(POM_IDENTITY_FILE, POM_PASSPHRASE);
	}
	
	@Test
	public void configureJsch_SettingsIdentityFile() {
		mojo.setServerId(SERVER_ID);
		mojo.configureJsch(log);
		assertIdentity(SETTINGS_IDENTITY_FILE, SETTINGS_PASSPHRASE);
	}
	
	@Test
	public void configureJsch_CustomIdentityOverridesPom() {
		mojo.setServerId(SERVER_ID);
		mojo.setPrivateKey(POM_IDENTITY_FILE);
		mojo.configureJsch(log);
		assertIdentity(POM_IDENTITY_FILE, SETTINGS_PASSPHRASE);
	}
	
	@Test
	public void configureJsch_CustomPassphraseOverridesPom() {
		mojo.setServerId(SERVER_ID);
		mojo.setPassphrase(POM_PASSPHRASE);
		mojo.configureJsch(log);
		assertIdentity(SETTINGS_IDENTITY_FILE, POM_PASSPHRASE);
	}
	
	@Test
	public void configureJsch_CustomKnownHosts() {
		mojo.setKnownHosts(KNOWN_HOSTS);
		mojo.configureJsch(log);
		final SshAgentSessionFactory factory = (SshAgentSessionFactory) JschConfigSessionFactory.getInstance();
		assertEquals(KNOWN_HOSTS, factory.getKnownHostsOrNull());
	}
}
