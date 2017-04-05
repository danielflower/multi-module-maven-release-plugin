package de.hilling.maven.release;

import scaffolding.TestProject;

import java.io.IOException;
import java.time.ZonedDateTime;

import org.eclipse.jgit.api.errors.GitAPIException;

import de.hilling.maven.release.versioning.ImmutableModuleVersion;
import de.hilling.maven.release.versioning.ImmutableQualifiedArtifact;
import de.hilling.maven.release.versioning.ImmutableReleaseInfo;
import de.hilling.maven.release.versioning.ImmutableFixVersion;
import de.hilling.maven.release.versioning.QualifiedArtifact;
import de.hilling.maven.release.versioning.ReleaseDateSingleton;
import de.hilling.maven.release.versioning.ReleaseInfo;
import de.hilling.maven.release.versioning.VersionMatcher;

public final class TestUtils {

    private TestUtils() {
    }

    public static final String TEST_GROUP_ID = "de.hilling.maven.release.testprojects";
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

    public static String tagNameStart() {
        final String tagName = "refs/tags/" + ReleaseDateSingleton.TAG_PREFIX + ZonedDateTime.now().withZoneSameInstant(
            ReleaseDateSingleton.RELEASE_DATE_TIMEZONE).format(ReleaseDateSingleton.FILE_SUFFIX_FORMATTER);
        return tagName.substring(0, tagName.length() - 3);
    }
}
