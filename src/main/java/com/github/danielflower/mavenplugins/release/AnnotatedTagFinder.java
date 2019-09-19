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

class AnnotatedTagFinder {
    private VersionNamer versionNamer;

    AnnotatedTagFinder(VersionNamer versionNamer) {
        this.versionNamer = versionNamer;
    }

    List<AnnotatedTag> tagsForVersion(Git git, String module, String versionWithoutBuildNumber) throws MojoExecutionException {
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
            if (isPotentiallySameVersionIgnoringBuildNumber(tagWithoutBuildNumber, tag.getName())) {
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

    boolean isPotentiallySameVersionIgnoringBuildNumber(String versionWithoutBuildNumber, String refName) {
        return buildNumberOf(versionWithoutBuildNumber, refName) != null;
    }

    String buildNumberOf(String versionWithoutBuildNumber, String refName) {
        String tagName = AnnotatedTag.stripRefPrefix(refName);
        String prefix = versionWithoutBuildNumber + versionNamer.getDelimiter();
        if (tagName.startsWith(prefix)) {
            return tagName.substring(prefix.length());
        }
        return null;
    }
}
