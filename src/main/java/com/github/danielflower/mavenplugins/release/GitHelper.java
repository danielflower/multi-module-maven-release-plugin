package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.util.ArrayList;
import java.util.List;

public class GitHelper {
    public static boolean hasLocalTag(Git repo, String tagToCheck) throws GitAPIException {
        return tag(repo, new EqualsMatcher(tagToCheck)) != null;
    }

    public static Ref refStartingWith(Git repo, final String tagPrefix) throws GitAPIException {
        return tag(repo, new Matcher() {
            @Override
            public boolean matches(String tagName) {
                return tagName.startsWith(tagPrefix);
            }
        });
    }

    private static Ref tag(Git repo, Matcher matcher) throws GitAPIException {
        for (Ref ref : repo.tagList().call()) {
            String currentTag = ref.getName().replace("refs/tags/", "");
            if (matcher.matches(currentTag)) {
                return ref;
            }
        }
        return null;
    }

    public static String scmUrlToRemote(String scmUrl) throws ValidationException {
        String GIT_PREFIX = "scm:git:";
        if (!scmUrl.startsWith(GIT_PREFIX)) {
            List<String> messages = new ArrayList<String>();
            String summary = "Cannot run the release plugin with a non-Git version control system";
            messages.add(summary);
            messages.add("The value in your scm tag is " + scmUrl);
            throw new ValidationException(summary, messages);
        }
        String remote = scmUrl.substring(GIT_PREFIX.length());
        remote  = remote.replace("file://localhost/", "file:///");
        return remote;
    }

    private interface Matcher {
        public boolean matches(String tagName);
    }

    private static class EqualsMatcher implements Matcher {
        private final String tagToCheck;

        public EqualsMatcher(String tagToCheck) {
            this.tagToCheck = tagToCheck;
        }

        @Override
        public boolean matches(String tagName) {
            return tagToCheck.equals(tagName);
        }
    }
}
