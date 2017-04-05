Contributing
------------

Pull requests are gratefully welcomed. It would be appreciated if the following guidelines are followed:

* Make sure tests cover your change, preferrably with unit tests. There are quite a few slower tests that run the plugin 
against sample projects in the `test-projects` folder. You amend existing tests or add a new one if necessary.
* Follow semantic versioning and increment the minor or major version number in `pom.xml` and
`scaffolding.TestProject.PLUGIN_VERSION_FOR_TESTS`
* Update the the changelog: `src/site/markdown/changelog.md`
* Use [.editorconfig](http://editorconfig.org/) to keep formatting consistent

Deploying to Nexus
------------------

**This section applies only to project owners**

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

**This section applies only to project owners**

The plugin uses itself to release itself.

    mvn smart-release:release

Note that for site generation you will need access to write to the Git repo and the following in your `settings.xml`:

    <server>
        <id>github</id>
        <username>GitHubLogin</username>
        <password>GitHubPassw0rd</password>
    </server>

Once released, go to the Nexus instance at https://oss.sonatype.org and log in, and then click on the "Staging Repositories"
link where you should find a repository in the list that looks something like `comgithubdanielflower-1010`. Select that
and then press the 'Close' button. After confirming, Nexus validates that all their requirements are met. Assuming that
passes, select the repo again and click 'Release'. Keep the default options and press 'Confirm'. It takes a little while
but will soon appear in the central Maven repo.
