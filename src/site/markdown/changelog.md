Changelog
---------

### 1.0.1

* Feature: A list of `releaseProfiles` can now be set in the plugin config.
* Bug fix: Support cases where Windows would return `C:\` and `c:\` for different calls in some situations, causing no sub-modules to build.
* Bug fix: When one commit has multiple tags, the wrong one would sometimes be seen as the latest and cause the release to fail.

### 1.0.0

First stable release.