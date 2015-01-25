THIS PROJECT IS IN DEVELOPMENT AND IS NOT READY FOR PRODUCTION USE
==================================================================

The Multi Module Maven Release Plugin for Git
=============================================

This plugin is an alternative to the `maven-release-plugin` which was created with the following principles:

* It should be trivial to release modules from a multi-module plugin, and only those modules that have changes should be released
* No commits should be made to a repo during a release
* Maven conventions such as developing against SNAPSHOT versions should be retained
* Git should not need to be installed on the system.

The plugin works with the idea that a software module has two types of versions: the "business version" and the
"release number". The business version is used for semantic versioning, and may be something like "1.0", "1.1", etc.
During development, the version in the pom is the business version with `-SNAPSHOT` appended. During a release, module
version becomes `business-version.release-number` and this is what the repo is tagged with, and this is what the
pom version becomes in the deployed artifact (however this version is not committed as a change to your pom).

Using a number that increments on each release - like your CI server's build number for example - is highly recommended
however it can be any number you want.

Usage
-----

Add the plugin to your pom:

    <build>
        <plugins>
            <plugin>
                <groupId>com.github.danielflower.mavenplugins</groupId>
                <artifactId>multi-module-maven-release-plugin</artifactId>
                <version>1.0.0</version>
                <configuration>
                    <releaseGoals>
                        <releaseGoal>install</releaseGoal>
                    </releaseGoals>
                </configuration>
            </plugin>
        </plugins>
    </build>

And then call the plugin with a release number:

	mvn releaser:release -DreleaseVersion=1234

Differences with the maven-release-plugin
-----------------------------------------

* Only Git is supported
* Each module released will have a separate tag with its artifact ID and version so that it is easy to see when a
version of a module was released
* A module is only released if there are changes to it
* The release version of the pom is not committed back to the repository
* Tests are run once by default (or optionally not at all)