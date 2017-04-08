package de.hilling.maven.release;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.util.FS;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author Roland Hauser sourcepond@gmail.com
 *
 */
public class SshAgentSessionFactoryTest {
	private static final String KNOWN_HOSTS = "known_hosts";
	private final Log log = mock(Log.class);
	private final FS fs = mock(FS.class);

	private String getFile(final String name) throws Exception {
		final URL url = getClass().getResource("/" + name);
		assertNotNull(format("File {} not found", name), url);
		return new File(url.toURI()).getAbsolutePath();
	}

	@Test
	public void createDefaultJSch_WithKnownHosts() throws Exception {
		final SshAgentSessionFactory factory = new SshAgentSessionFactory(log, KNOWN_HOSTS, null, null);
		factory.setKnownHosts(getFile(KNOWN_HOSTS));
		final JSch jsch = factory.createDefaultJSch(fs);
		final HostKey[] keys = jsch.getHostKeyRepository().getHostKey("github.com", "ssh-rsa");
		assertEquals(1, keys.length);
	}
}
