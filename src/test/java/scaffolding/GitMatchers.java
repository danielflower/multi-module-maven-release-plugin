package scaffolding;

import com.github.danielflower.mavenplugins.release.GitHelper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.List;

import static org.eclipse.jgit.lib.Constants.R_TAGS;

public class GitMatchers {

    public static Matcher<Git> hasTag(final String tag) {
        return new TypeSafeDiagnosingMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git repo, Description mismatchDescription) {
                try {
                    mismatchDescription.appendValueList("a git repo with tags: ", ", ", "", repo.getRepository().getTags().keySet());
                    return GitHelper.hasLocalTag(repo, tag);
                } catch (Exception e) {
                    throw new RuntimeException("Couldn't access repo", e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a git repo with the tag " + tag);
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
