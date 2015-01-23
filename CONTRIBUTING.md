
Deploying to Nexus
------------------

The JARs must be signed and you must have access to upload to Sonatype, so you need a GPG key and a Sonatype login.
The passwords for this should be in your Maven `settings.xml` with the following config:

	<settings>
      <servers>
        <server>
          <id>sonatype-nexus-staging</id>
          <username>your sonatype nexus username</username>
          <password>your password</password>
        </server>
        <server>
          <id>sonatype-nexus-snapshots</id>
          <username>your sonatype nexus username</username>
          <password>your password</password>
        </server>
      </servers>
      <profiles>
        <profile>
          <id>gpg</id>
          <properties>
    	    <gpg.keyname>your public key name</gpg.keyname>
            <gpg.passphrase>your key passphrase</gpg.passphrase>
          </properties>
        </profile>
      </profiles>
    </settings>

Given everything is set up correctly, the following deploys a snapshot version:

    mvn clean deploy -P release,gpg
