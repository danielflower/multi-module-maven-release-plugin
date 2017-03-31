package com.github.danielflower.mavenplugins.release;

import scaffolding.TestProject;

import java.io.IOException;
import java.time.ZonedDateTime;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.versioning.ImmutableModuleVersion;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableQualifiedArtifact;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableReleaseInfo;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableFixVersion;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;
import com.github.danielflower.mavenplugins.release.versioning.VersionMatcher;

public class TestUtils {

    public static final String TEST_GROUP_ID = "com.github.danielflower.mavenplugins.testprojects";

    public static ReleaseInfo releaseInfo(long major, long minor, String tagName, String moduleName) {
        final ImmutableReleaseInfo.Builder builder = ImmutableReleaseInfo.builder();
        builder.tagName(tagName);
        final ImmutableModuleVersion.Builder versionBuilder = ImmutableModuleVersion.builder();
        versionBuilder.releaseDate(ZonedDateTime.now());
        final ImmutableQualifiedArtifact.Builder artifactBuilder = ImmutableQualifiedArtifact.builder();
        artifactBuilder.artifactId(moduleName);
        artifactBuilder.groupId(TEST_GROUP_ID);
        versionBuilder.artifact(artifactBuilder.build());
        builder.addModules(versionBuilder.version(ImmutableFixVersion.builder().majorVersion(major).minorVersion
                                                                                                        (minor).build
                                                                                                                    ()).build());
        return builder.build();
    }

    static AnnotatedTag saveFileInModule(TestProject project, String moduleName, String version) throws
                                                                                                                   IOException,
                                                                                                                   GitAPIException {
        project.commitRandomFile(moduleName);
        String nameForTag = moduleName.equals(".") ? "root" : moduleName;
        return tagLocalRepo(project, nameForTag + "-" + version , version);
    }

    private static AnnotatedTag tagLocalRepo(TestProject project, String tagName, String version) throws GitAPIException {
        final ImmutableReleaseInfo.Builder builder = ImmutableReleaseInfo.builder();
        builder.tagName(tagName);
        final ImmutableModuleVersion.Builder versionBuilder = ImmutableModuleVersion.builder();
        final ImmutableQualifiedArtifact.Builder artifactBuilder = ImmutableQualifiedArtifact.builder();
        artifactBuilder.groupId(TEST_GROUP_ID);
        artifactBuilder.artifactId(project.getArtifactId());
        versionBuilder.artifact(artifactBuilder.build());
        versionBuilder.version(new VersionMatcher(version).fixVersion());
        versionBuilder.releaseDate(ZonedDateTime.now());
        builder.addModules(versionBuilder.build());
        AnnotatedTag tag = new AnnotatedTag(null, tagName, builder.build());
        tag.saveAtHEAD(project.local);
        return tag;
    }


}
