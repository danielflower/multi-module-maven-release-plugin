package scaffolding;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class GitMatchers {

    public static Matcher<Repository> hasTag(final String tag) {
        return new TypeSafeMatcher<Repository>() {
            @Override
            protected boolean matchesSafely(Repository repo) {
                Git git = new Git(repo);
                ListTagCommand listTagCommand = git.tagList();
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

}
