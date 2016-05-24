package com.github.danielflower.mavenplugins.release;

import static com.github.danielflower.mavenplugins.release.NextMojo.getRemoteUrlOrNullIfNoneSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
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
public class NextMojoTest {
	private static final String DEVELOPER_CONNECTION = "scm:git:ssh://some/developerPath";
	private static final String CONNECTION = "scm:git:ssh://some/commonPath";
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
	private final Scm scm = mock(Scm.class);
	private NextMojo mojo;

	@Before
	public void setup() {
		mojo = new NextMojo();
		mojo.setRepository(repository);
		mojo.setReactorBuilderFactory(reactorBuilderFactory);
		mojo.setLogHolder(logHolder);
		mojo.project = mock(MavenProject.class);

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

	@Test
	public void getRemoteUrlScmIsNull() throws PluginException {
		assertNull(getRemoteUrlOrNullIfNoneSet(null));
	}

	@Test
	public void getRemoteUrlNoConnectionsOnScm() throws PluginException {
		assertNull(getRemoteUrlOrNullIfNoneSet(scm));
	}

	@Test
	public void getRemoteUrlUseDeveloperConnection() throws PluginException {
		when(scm.getDeveloperConnection()).thenReturn(DEVELOPER_CONNECTION);
		when(scm.getConnection()).thenReturn(CONNECTION);
		assertEquals("ssh://some/developerPath", getRemoteUrlOrNullIfNoneSet(scm));
	}

	@Test
	public void getRemoteUrlUseConnection() throws PluginException {
		when(scm.getConnection()).thenReturn(CONNECTION);
		assertEquals("ssh://some/commonPath", getRemoteUrlOrNullIfNoneSet(scm));
	}

	@Test
	public void getRemoteUrlIllegalProtocol() {
		when(scm.getDeveloperConnection()).thenReturn("scm:svn:ssh//some/illegal/protocol");
		try {
			getRemoteUrlOrNullIfNoneSet(scm);
			fail("Exception expected");
		} catch (final PluginException expected) {
			assertEquals("Cannot run the release plugin with a non-Git version control system", expected.getMessage());
			final List<String> messages = expected.getMessages();
			assertEquals(2, messages.size());
			assertEquals("Cannot run the release plugin with a non-Git version control system", messages.get(0));
			assertEquals("The value in your scm tag is scm:svn:ssh//some/illegal/protocol", messages.get(1));
		}
	}
}
