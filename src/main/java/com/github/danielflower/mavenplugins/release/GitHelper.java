package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

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
