package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

public class GitHelper {
    public static boolean hasLocalTag(Git repo, String tagToCheck) throws GitAPIException {
        ListTagCommand listTagCommand = repo.tagList();
        String targetRefName = "refs/tags/" + tagToCheck;
        for (Ref ref : listTagCommand.call()) {
            String tagName = ref.getName();
            if (tagName.equals(targetRefName)) {
                return true;
            }
        }
        return false;
    }
}
