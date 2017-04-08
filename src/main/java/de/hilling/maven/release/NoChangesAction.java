package de.hilling.maven.release;

/**
 * What should be done if no changes detected.
 */
public enum NoChangesAction {
    ReleaseAll, ReleaseNone, FailBuild;
}
