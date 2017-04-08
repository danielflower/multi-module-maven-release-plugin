Documentation, download, and usage instructions
===============================================

**Automatically releases only changed modules of a multi-module maven project.**

This is fork of Daniel Flowers great 
[multi-module-maven-release-plugin](http://danielflower.github.io/multi-module-maven-release-plugin/index.html).

This plugin adds bugfix release feature.

Development
===========

[![Build Status](https://travis-ci.org/guhilling/smart-release-plugin.svg?branch=master)](https://travis-ci.org/guhilling/smart-release-plugin)

Contributing
------------

To build and run the tests, you need Java 8 or later and Maven 3 or later. Simply clone and run `mvn install`

Note that the tests run the plugin against a number of sample test projects, located in the `test-projects` folder.
If adding new functionality, or fixing a bug, it is recommended that a sample project be set up so that the scenario
can be tested end-to-end.

See also [CONTRIBUTING.md](CONTRIBUTING.md) for information on deploying to Nexus and releasing the plugin.

Features
--------

* Automatically releases only changed modules of a multi-module maven project.
    * Dependencies are automatically resolved transitively.
    * Version numbers must follow format <Major>-SNAPSHOT.
    * Minor and bugfix numbers are chosen automatically.
    * Regular releases increase the minor number in the resulting artifacts.
    * Bugfix releases increase the bugfix number relative to the latest regular release.
* Allows to create bugfix-releases in bugfix branches.
    * Use flag -DperformBugfixRelease to trigger bugfix.
* Tracks the released versions robust and efficient in release-files.

Stability stuff
---------------

* Figure out if things like MVN_OPTIONS and other JVM options need to be passed during release
