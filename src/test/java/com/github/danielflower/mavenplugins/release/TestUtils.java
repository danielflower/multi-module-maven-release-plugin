package com.github.danielflower.mavenplugins.release;

import scaffolding.TestProject;

import java.io.IOException;
import java.time.ZonedDateTime;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.versioning.ImmutableModuleVersion;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableQualifiedArtifact;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableReleaseInfo;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableFixVersion;
import com.github.danielflower.mavenplugins.release.versioning.QualifiedArtifact;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;
import com.github.danielflower.mavenplugins.release.versioning.VersionMatcher;

public class TestUtils {

    public static final String TEST_GROUP_ID = "com.github.danielflower.mavenplugins.testprojects";
    private static final String TEST_TAG_NAME = "test-tag";

    public static ReleaseInfo releaseInfo(long major, long minor, String tagName, String moduleName) {
        final ImmutableReleaseInfo.Builder builder = ImmutableReleaseInfo.builder();
        builder.tagName(tagName);
        final ImmutableModuleVersion.Builder versionBuilder = ImmutableModuleVersion.builder();
        versionBuilder.releaseDate(ZonedDateTime.now());
        final ImmutableQualifiedArtifact.Builder artifactBuilder = ImmutableQualifiedArtifact.builder();
        artifactBuilder.artifactId(moduleName);
        artifactBuilder.groupId(TEST_GROUP_ID);
        versionBuilder.artifact(artifactBuilder.build());
        final ImmutableFixVersion fixVersion = ImmutableFixVersion.builder().majorVersion(major).minorVersion(minor)
                                                                  .build();
        final ImmutableModuleVersion moduleVersion = versionBuilder.version(fixVersion).releaseTag("test-tag").build();
        builder.addModules(moduleVersion);
        return builder.build();
    }

    static AnnotatedTag saveFileInModule(TestProject project, String moduleName, String version) throws IOException,
                                                                                                        GitAPIException {
        project.commitRandomFile(moduleName);
        String nameForTag = moduleName.equals(".")
                            ? "root"
                            : moduleName;
        return tagLocalRepo(project, nameForTag + "-" + version, version);
    }

    public static QualifiedArtifact artifactIdForModule(String module) {
        return ImmutableQualifiedArtifact.builder().groupId(TEST_GROUP_ID).artifactId(module).build();
    }

    private static AnnotatedTag tagLocalRepo(TestProject project, String tagName, String version) throws
                                                                                                  GitAPIException {
        final ImmutableReleaseInfo.Builder builder = ImmutableReleaseInfo.builder();
        builder.tagName(tagName);
        final ImmutableModuleVersion.Builder versionBuilder = ImmutableModuleVersion.builder();
        final ImmutableQualifiedArtifact.Builder artifactBuilder = ImmutableQualifiedArtifact.builder();
        artifactBuilder.groupId(TEST_GROUP_ID);
        artifactBuilder.artifactId(project.getArtifactId());
        versionBuilder.artifact(artifactBuilder.build());
        versionBuilder.version(new VersionMatcher(version).fixVersion());
        versionBuilder.releaseTag(TEST_TAG_NAME);
        versionBuilder.releaseDate(ZonedDateTime.now());
        builder.addModules(versionBuilder.build());
        AnnotatedTag tag = new AnnotatedTag(null, tagName, builder.build());
        tag.saveAtHEAD(project.local);
        return tag;
    }

    public static ImmutableFixVersion fixVersion(int majorVersion, int minorVersion) {
        return ImmutableFixVersion.builder().majorVersion(majorVersion).minorVersion(minorVersion).build();
    }
}
