Documentation, download, and usage instructions
===============================================

Full usage details, FAQs, background and more are available on the
**[project documention website](http://danielflower.github.io/multi-module-maven-release-plugin/index.html)**.

Development
===========

[![Build Status](https://travis-ci.org/danielflower/multi-module-maven-release-plugin.svg?branch=master)](https://travis-ci.org/danielflower/multi-module-maven-release-plugin)

Contributing
------------

To build and run the tests, you need Java 6 or later and Maven 3 or later. Simply clone and run `mvn install`

Note that the tests run the plugin against a number of sample test projects, located in the `test-projects` folder.
If adding new functionality, or fixing a bug, it is recommended that a sample project be set up so that the scenario
can be tested end-to-end.

See also [CONTRIBUTING.md](CONTRIBUTING.md) for information on deploying to Nexus and releasing the plugin.

TODO
====

Features
--------

* Default build number to an incremented value based on a previous tag
* Allow easy way to bump minor or major versions of a module (during release?)
* Change build-number to a long to enforce correctly ordered dependencies and allow an optional label that is appended to the version
* Allow optional appending of branch name to release version if not "master" (or supplied regex)

Stability stuff
---------------

* Figure out if things like MVN_OPTIONS and other JVM options need to be passed during release
* Run E2E tests against multiple Maven versions
* Have a retry on all remote-git operations to cover flaky servers (?)
* Test the plugin on very large repositories
* Tests on partial-releases:
    * Make sure the diffdetector works correctly with branches, specifically when one brance has a change another doesn't, and when two branches have changes
