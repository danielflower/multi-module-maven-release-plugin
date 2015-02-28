Release Plugin TODOs
====================

[![Build Status](https://travis-ci.org/danielflower/multi-module-maven-release-plugin.svg?branch=master)](https://travis-ci.org/danielflower/multi-module-maven-release-plugin)

Features
--------

* Do not re-release a module if it hasn't changed since the last one
* Allow easy way to bump minor or major versions of a module (during release?)
* Change build-number to a long to enforce correctly ordered dependencies and allow an optional label that is appended to the version
* Allow optional appending of branch name to release version if not "master" (or supplied regex)

Stability stuff
---------------

* Use the value in the scm tag for the remote URL when pushing, if it's available
* Figure out if things like MVN_OPTIONS and other JVM options need to be passed during release
* Test more nested projects work
* Test cases where things like group IDs in parents are omitted
* Run E2E tests against multiple Maven versions
* Tests on partial-releases:
    * Make sure the diffdetector works correctly with branches, specifically when one brance has a change another doesn't, and when two branches have changes
    * If there are no changes to release, the whole project should be re-released
    * If A->B->C, and B hasn't changed, but C has, then B should still be rebuilt and A should therefore get the new C
    * Nested modules
    * Cases where module path != artifact ID