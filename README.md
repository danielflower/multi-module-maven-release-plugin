Documentation, download, and usage instructions
===============================================

Full usage details, FAQs, background and more are available on the
**[project documentation website](https://danielflower.github.io/multi-module-maven-release-plugin/index.html)**.

Development
===========

[![Build Status](https://travis-ci.org/danielflower/multi-module-maven-release-plugin.svg?branch=master)](https://travis-ci.org/danielflower/multi-module-maven-release-plugin) ![Maven Central](https://img.shields.io/maven-central/v/com.github.danielflower.mavenplugins/multi-module-maven-release-plugin.svg)


Contributing
------------

To build and run the tests, you need Java 8 or later and Maven 3 or later. Simply clone and run `mvn install`

Note that the tests run the plugin against a number of sample test projects, located in the `test-projects` folder.
If adding new functionality, or fixing a bug, it is recommended that a sample project be set up so that the scenario
can be tested end-to-end.

See also [CONTRIBUTING.md](CONTRIBUTING.md) for information on deploying to Nexus and releasing the plugin.
