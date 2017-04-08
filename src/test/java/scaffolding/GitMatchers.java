package scaffolding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import de.hilling.maven.release.AnnotatedTag;
import de.hilling.maven.release.GitHelper;
import de.hilling.maven.release.versioning.ImmutableFixVersion;
import de.hilling.maven.release.versioning.ImmutableModuleVersion;
import de.hilling.maven.release.versioning.ImmutableQualifiedArtifact;
import de.hilling.maven.release.versioning.VersionMatcher;

public class GitMatchers {

    public static Matcher<Git> hasTag(final String tag) {
        return new TypeSafeDiagnosingMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git repo, Description mismatchDescription) {
                try {
                    mismatchDescription
                        .appendValueList("a git repo with tags: ", ", ", "", repo.getRepository().getTags().keySet());
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

    public static Matcher<Git> hasTagWithModuleVersion(String groupId, final String moduleName, String version) {
        final ImmutableFixVersion expectedVersion = new VersionMatcher(version).fixVersion();
        final ImmutableQualifiedArtifact artifact = ImmutableQualifiedArtifact.builder().groupId(groupId)
                                                                              .artifactId(moduleName).build();
        return new TypeSafeDiagnosingMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git repo, Description mismatchDescription) {
                try {
                    final ArrayList<String> foundVersions = new ArrayList<>();
                    for (Ref ref : repo.tagList().call()) {
                        final AnnotatedTag tag = AnnotatedTag.fromRef(repo.getRepository(), ref);
                        final Optional<ImmutableModuleVersion> version = tag.getReleaseInfo()
                                                                            .versionForArtifact(artifact);
                        if (version.isPresent()) {
                            final ImmutableModuleVersion moduleVersion = version.get();
                            if (moduleVersion.getVersion().equals(expectedVersion)) {
                                return true;
                            } else {
                                foundVersions.add(moduleVersion.getVersion().toString());
                            }
                        }
                    }
                    mismatchDescription
                        .appendValueList("a git repo containing tags with module versions [", ", ", "]", foundVersions);
                    return false;
                } catch (GitAPIException | IOException e) {
                    throw new RuntimeException("Couldn't access repo", e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(
                    "a git repo with tag containing module '" + moduleName + "' " + "with version " + expectedVersion
                                                                                                          .toString());
            }
        };
    }

    public static Matcher<Git> isInSynchWithOrigin() {
        return new TypeSafeDiagnosingMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git git, Description mismatchDescription) {
                try {
                    Repository repo = git.getRepository();
                    ObjectId fetchHead = repo.resolve("origin/master^{tree}");
                    ObjectId head = repo.resolve("HEAD^{tree}");

                    ObjectReader reader = repo.newObjectReader();
                    CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                    oldTreeIter.reset(reader, head);
                    CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                    newTreeIter.reset(reader, fetchHead);
                    List<DiffEntry> diffs = git.diff().setShowNameAndStatusOnly(true).setNewTree(newTreeIter)
                                               .setOldTree(oldTreeIter).call();
                    if (diffs.isEmpty()) {
                        return true;
                    } else {
                        String start = "Detected the following changes in " + git.getRepository().getDirectory()
                                                                                 .getCanonicalPath() + ": ";
                        String end = ".";
                        mismatchDescription.appendValueList(start, ", ", end, diffs.stream().map(DiffEntry::toString)
                                                                                   .collect(Collectors.toList()));
                        return false;
                    }
                } catch (GitAPIException | IOException e) {
                    throw new RuntimeException("Error checking git status", e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("A git directory with no difference to its origin");
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
