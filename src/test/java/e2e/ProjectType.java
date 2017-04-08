package e2e;

public enum ProjectType {
    SINGLE("single-module"),
    NESTED("nested-project"),
    TAGGED_MODULE("module-with-scm-tag"),
    MODULE_PROFILES("module-with-profiles"),
    INHERITED_VERSIONS("inherited-versions-from-parent"),
    INDEPENDENT_VERSIONS("independent-versions"),
    INDEPENDENT_VERSIONS_BUGFIX("independent-versions-bugfix"),
    PARENT_AS_SIBLING("parent-as-sibling"),
    DEEP_DEPENDENCIES("deep-dependencies"),
    MODULE_WITH_TEST_FAILURE("module-with-test-failure"),
    SNAPSHOT_DEPENDENCIES("snapshot-dependencies"),
    SNAPSHOT_DEPENDENCIES_VIA_PROPERTIES("snapshot-dependencies-with-version-properties");

    private final String submoduleName;

    ProjectType(String submoduleName) {
        this.submoduleName = submoduleName;
    }

    public String getSubmoduleName() {
        return submoduleName;
    }
}
