package com.github.danielflower.mavenplugins.release;

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
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;


/**
 * SSH-Agent enabler.
 *
 * A JschConfigSessionFactory which sets the Preferred authentication method to Publickey,
 * and tries to use a NetCat Socket Factory to reach a ssh-agent process via it's Unix Socket
 * as identified by environment var SSH_AUTH_SOCK.
 *
 * Note that this requires the 'nc' binary installed and available on PATH!
 *
 * @author Johan Ström <johan@pistonlabs.com>
 */
public class SshAgentSessionFactory extends JschConfigSessionFactory {
    private final Log log;

    public SshAgentSessionFactory(Log log) {
        this.log = log;
    }

    @Override
    protected void configure(OpenSshConfig.Host host, Session sn) {
    }

    @Override
    protected JSch createDefaultJSch(FS fs) throws JSchException {
        Connector con = null;
        try {
            // TODO: add support for others as well, such as page-ant.
            if (SSHAgentConnector.isConnectorAvailable()) {
                USocketFactory usf = new NCUSocketFactory();
                con = new SSHAgentConnector(usf);
            }
        } catch (AgentProxyException e) {
            log.warn("Failed to connect to SSH-agent", e);
        }

        final JSch jsch = super.createDefaultJSch(fs);
        if (con != null) {
            JSch.setConfig("PreferredAuthentications", "publickey");

            IdentityRepository identityRepository = new RemoteIdentityRepository(con);
            jsch.setIdentityRepository(identityRepository);

            log.debug("Jsch configured to use "+con.getName());
        }

        return jsch;
    }
}
