package scaffolding;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class GitMatchers {

    public static Matcher<Git> hasTag(final String tag) {
        return new TypeSafeMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git repo) {
                ListTagCommand listTagCommand = repo.tagList();
                try {
                    String targetRefName = "refs/tags/" + tag;
                    for (Ref ref : listTagCommand.call()) {
                        String tagName = ref.getName();
                        if (tagName.equals(targetRefName)) {
                            return true;
                        }
                    }
                } catch (GitAPIException e) {
                    throw new RuntimeException("Couldn't access repo", e);
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a git repo with the tag " + tag);
            }
        };
    }

    public static Matcher<Git> hasCleanWorkingDirectory() {
        return new TypeSafeMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git git) {
                try {
                    return git.status().call().isClean();
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
