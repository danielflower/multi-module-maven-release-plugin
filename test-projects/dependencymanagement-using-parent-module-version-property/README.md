In this project the aggregator pom is separate from the parent pom of the modules in this project.

Other modules reference the parent pom using a relative path, version is set by ${parent-module.version}.

This is a malformed maven project, as parent version should not be set by a property.
Build even reports a warning saying that such malformed project may not compile any more in future.

But in a scenario with a huge multi module project being developed on many branches with different versions, this significantly simplifies merging, as there is not module version related merge conflicts in pom files. 