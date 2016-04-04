package com.github.danielflower.mavenplugins.release;

import static java.lang.String.format;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import com.jcraft.jsch.agentproxy.USocketFactory;
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector;
import com.jcraft.jsch.agentproxy.usocket.NCUSocketFactory;

/**
 * SSH-Agent enabler.
 *
 * A JschConfigSessionFactory which sets the Preferred authentication method to
 * Publickey, and tries to use a NetCat Socket Factory to reach a ssh-agent
 * process via it's Unix Socket as identified by environment var SSH_AUTH_SOCK.
 *
 * Note that this requires the 'nc' binary installed and available on PATH!
 *
 * @author Johan Ström johan@pistonlabs.com
 */
public class SshAgentSessionFactory extends JschConfigSessionFactory {
	private final Log log;
	private String knownHostsOrNull;
	private String identityFile;
	private String passphraseOrNull;

	public SshAgentSessionFactory(final Log log, final String knownHostsOrNull, final String identityFile,
			final String passphraseOrNull) {
		this.log = log;
		setKnownHosts(knownHostsOrNull);
		setIdentityFile(identityFile);
		setPassphrase(passphraseOrNull);
	}

	void setKnownHosts(String knownHosts) {
		this.knownHostsOrNull = knownHosts;
	}

	void setIdentityFile(String identityFile) {
		this.identityFile = identityFile;
	}

	void setPassphrase(String passphrase) {
		this.passphraseOrNull = passphrase;
	}

	String getKnownHostsOrNull() {
		return knownHostsOrNull;
	}

	String getIdentityFile() {
		return identityFile;
	}

	String getPassphraseOrNull() {
		return passphraseOrNull;
	}

	@Override
	protected void configure(final OpenSshConfig.Host host, final Session sn) {
	}

	@Override
	protected JSch createDefaultJSch(final FS fs) throws JSchException {
		Connector con = null;
		try {
			// TODO: add support for others as well, such as page-ant.
			if (SSHAgentConnector.isConnectorAvailable()) {
				final USocketFactory usf = new NCUSocketFactory();
				con = new SSHAgentConnector(usf);
			}
		} catch (final AgentProxyException e) {
			log.warn("Failed to connect to SSH-agent", e);
		}

		final JSch jsch = super.createDefaultJSch(fs);
		if (con != null) {
			JSch.setConfig("PreferredAuthentications", "publickey");

			final IdentityRepository identityRepository = new RemoteIdentityRepository(con);
			jsch.setIdentityRepository(identityRepository);

			log.debug(format("Jsch configured to use %s", con.getName()));
		}

		if (knownHostsOrNull != null) {
			jsch.setKnownHosts(knownHostsOrNull);
			log.debug(format("Jsch configured to use known hostfile %s", knownHostsOrNull));
		}

		if (identityFile != null) {
			jsch.addIdentity(identityFile, passphraseOrNull);
			log.debug(format("Jsch configured to use identity file %s", identityFile));
		}

		return jsch;
	}
}
