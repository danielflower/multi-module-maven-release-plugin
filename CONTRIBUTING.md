
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

    mvn com.github.danielflower.mavenplugins:multi-module-maven-release-plugin:0.1.12:release -DreleaseVersion=3 -P release,gpg

Once released, go to the Nexus instance at https://oss.sonatype.org and log in, and then click on the "Staging Repositories"
link where you should find a repository in the list that looks something like `comgithubdanielflower-1010`. Select that
and then press the 'Close' button. After confirming, Nexus validates that all their requirements are met. Assuming that
passes, select the repo again and click 'Release'. Keep the default options and press 'Confirm'. It takes a little while
but will soon appear in the central Maven repo.
