package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnnotatedTagFinder {

    public static List<AnnotatedTag> mostRecent(Git git, String module, String versionWithoutBuildNumber) throws GitAPIException, IOException {
        ArrayList<AnnotatedTag> results = new ArrayList<AnnotatedTag>();
        List<Ref> tags = git.tagList().call();
        Collections.reverse(tags);
        for (Ref tag : tags) {
            if (isPotentiallySameVersionIgnoringBuildNumber(module + "-" + versionWithoutBuildNumber, tag.getName())) {
                results.add(AnnotatedTag.fromRef(git.getRepository(), tag));
                break;
            }
        }
        return results;
    }

    static boolean isPotentiallySameVersionIgnoringBuildNumber(String versionWithoutBuildNumber, String refName) {
        String tagName = AnnotatedTag.stripRefPrefix(refName);
        return tagName.startsWith(versionWithoutBuildNumber + ".");
    }
}
