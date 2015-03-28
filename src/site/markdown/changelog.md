Changelog
---------

### 1.0.1

* BUGFIX: Support cases where Windows would return `C:\` and `c:\` for different calls when using Git bash, causing no sub-modules to build.
* BUGFIX: When one commit has multiple tags, the wrong one would sometimes be seen as the latest and cause the release to fail.

### 1.0.0

First stable release.