package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnnotatedTagFinder {

    public static List<AnnotatedTag> tagsForVersion(Git git, String module, String versionWithoutBuildNumber, String delimiter) throws MojoExecutionException {
        ArrayList<AnnotatedTag> results = new ArrayList<AnnotatedTag>();
        List<Ref> tags;
        try {
            tags = git.tagList().call();
        } catch (GitAPIException e) {
            throw new MojoExecutionException("Error while getting a list of tags in the local repo", e);
        }
        Collections.reverse(tags);
        String tagWithoutBuildNumber = module + "-" + versionWithoutBuildNumber;
        for (Ref tag : tags) {
            if (isPotentiallySameVersionIgnoringBuildNumber(tagWithoutBuildNumber, tag.getName(), delimiter)) {
                try {
                    results.add(AnnotatedTag.fromRef(git.getRepository(), tag));
                } catch (IncorrectObjectTypeException ignored) {
                    // not actually a tag, so skip it.
                } catch (IOException e) {
                    throw new MojoExecutionException("Error while looking up tag " + tag, e);
                }
            }
        }
        return results;
    }

    public static boolean isPotentiallySameVersionIgnoringBuildNumber(String versionWithoutBuildNumber, String refName, String delimiter) {
        return buildNumberOf(versionWithoutBuildNumber, refName, delimiter) != null;
    }

    public static Long buildNumberOf(String versionWithoutBuildNumber, String refName, String delimiter) {
        String tagName = AnnotatedTag.stripRefPrefix(refName);
        String prefix = versionWithoutBuildNumber + delimiter;
        if (tagName.startsWith(prefix)) {
            String end = tagName.substring(prefix.length());
            try {
                return Long.parseLong(end);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

}
