Release Plugin TODOs
====================

[![Build Status](https://travis-ci.org/danielflower/multi-module-maven-release-plugin.svg?branch=master)](https://travis-ci.org/danielflower/multi-module-maven-release-plugin)

Features
--------

* Do not re-release a module if it hasn't changed since the last one
* Allow build numbers to be numbers, strings, or number-string format
* Allow optional appending of branch name to release version if not "master" (or supplied regex)
* Allow easy way to bump minor or major versions of a module (during release?)

Validationy stuff
-----------------

* Throw clear error early if there are any snapshot versions for plugins

Stability stuff
---------------

* Make sure it can be run from any folder
* Use the value in the scm tag for the remote URL when pushing, if it's available
* Figure out if things like MVN_OPTIONS and other JVM options need to be passed during release
* Test more nested projects work
* Test cases where things like group IDs in parents are omitted
* If it crashes it should report exactly what it has done and what it hasn't
