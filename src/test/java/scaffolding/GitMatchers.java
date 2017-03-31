package scaffolding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import com.github.danielflower.mavenplugins.release.AnnotatedTag;
import com.github.danielflower.mavenplugins.release.GitHelper;
import com.github.danielflower.mavenplugins.release.TestUtils;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableFixVersion;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableModuleVersion;
import com.github.danielflower.mavenplugins.release.versioning.ImmutableQualifiedArtifact;
import com.github.danielflower.mavenplugins.release.versioning.VersionMatcher;

public class GitMatchers {

    public static Matcher<Git> hasTag(final String tag) {
        return new TypeSafeDiagnosingMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git repo, Description mismatchDescription) {
                try {
                    mismatchDescription.appendValueList("a git repo with tags: ", ", ", "", repo.getRepository().getTags().keySet());
                    return GitHelper.hasLocalTag(repo, tag);
                } catch (GitAPIException e) {
                    throw new RuntimeException("Couldn't access repo", e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a git repo with the tag " + tag);
            }
        };
    }

    public static Matcher<Git> hasTagWithModuleVersion(final String moduleName, String version) {
        final ImmutableFixVersion expectedVersion = new VersionMatcher(version).fixVersion();
        final ImmutableQualifiedArtifact artifact = ImmutableQualifiedArtifact.builder().groupId(TestUtils.TEST_GROUP_ID)
                                                                           .artifactId(moduleName).build();
        return new TypeSafeDiagnosingMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git repo, Description mismatchDescription) {
                try {
                    final ArrayList<String> foundVersions = new ArrayList<>();
                    for (Ref ref : repo.tagList().call()) {
                        final AnnotatedTag tag = AnnotatedTag.fromRef(repo.getRepository(), ref);
                        final Optional<ImmutableModuleVersion> version = tag.getReleaseInfo()
                                                                                           .versionForArtifact(
                                                                                               artifact);
                        if (version.isPresent()) {
                            final ImmutableModuleVersion moduleVersion = version.get();
                            if(moduleVersion.getVersion().equals(expectedVersion)) {
                                return true;
                            } else {
                                foundVersions.add(moduleVersion.toString());
                            }
                        }
                    }
                    mismatchDescription.appendValueList("a git repo containing tags with module versions [", ", ",
                                                        "]", foundVersions);
                    return false;
                } catch (GitAPIException|IOException e) {
                    throw new RuntimeException("Couldn't access repo", e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a git repo with tag containing module '" + moduleName + "' " +
                                           "with version " + expectedVersion.toString());
            }
        };
    }

    public static Matcher<Git> hasCleanWorkingDirectory() {
        return new TypeSafeDiagnosingMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git git, Description mismatchDescription) {
                try {
                    Status status = git.status().call();
                    if (!status.isClean()) {
                        String start = "Uncommitted changes in ";
                        String end = " at " + git.getRepository().getWorkTree().getAbsolutePath();
                        mismatchDescription.appendValueList(start, ", ", end, status.getUncommittedChanges());
                    }
                    return status.isClean();
                } catch (GitAPIException e) {
                    throw new RuntimeException("Error checking git status", e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("A git directory with no staged or unstaged changes");
            }
        };
    }
}
