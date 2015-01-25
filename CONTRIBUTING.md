
Deploying to Nexus
------------------

The JARs must be signed and you must have access to upload to Sonatype, so you need a GPG key and a Sonatype login.
The passwords for this should be in your Maven `settings.xml` with the following config:

	<settings>
      <servers>
        <server>
          <id>ossrh</id>
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

Performing a release
--------------------

The plugin can use itself to release itself.

    mvn com.github.danielflower.mavenplugins:multi-module-release-maven-plugin:release -DreleaseVersion=3 -P release,gpg

Once released, go to the Nexus instance at https://oss.sonatype.org and close the release in the "Staging Repositories" link.

Full instructions: https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide
