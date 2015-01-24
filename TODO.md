Release Plugin TODOs
====================

[![Build Status](https://travis-ci.org/danielflower/maven-multi-module-releaser.svg?branch=master)](https://travis-ci.org/danielflower/maven-multi-module-releaser)

Features
--------

* Multi-module projects, with dependencies amongst them. Add one tag per module
* Shared/inherited version numbers, or independent version numbers
* Allow easy way to bump minor or major versions of a module (during release?)
* Do not re-release a module if it hasn't changed since the last one
* Guess a good value for the release version if it's not specified
* Allow releaseVersions to be numbers, strings, or number-string format
* Allow optional appending of branch name to release version if not "master" (or supplied regex)

Validationy stuff
-----------------

* Make the user-error messages big and clear
* Throw clear error early if release is not in git repo
* Throw clear error early if there are any snapshot versions for parents, dependencies or plugins
* Throw clear error if working set is not clean
* Throw clear error early if tag already exists (what if it exists locally but not remotely? or other way round?)

Stability stuff
---------------

* Make sure it can be run from any folder
* Use the value in the scm tag for the remote URL when pushing, if it's available
* Figure out if things like MVN_OPTIONS and other JVM options need to be passed during release