package com.github.danielflower.mavenplugins.release;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.Ref;

public class AnnotatedTagFinder {

    private static final Pattern SINGLE_NUMBER                 = Pattern.compile("\\d+");
    private static final Pattern SINGLE_OR_MAJOR_MINOR_VERSION = Pattern.compile("(?<first>\\d+)(\\.(?<second>\\d+))?");

    private final boolean findBugfixReleases;

    public AnnotatedTagFinder(boolean findBugfixReleases) {
        this.findBugfixReleases = findBugfixReleases;
    }

    public List<AnnotatedTag> tagsForVersion(Git git, String module, String versionWithoutBuildNumber) throws
                                                                                                       MojoExecutionException {
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

    // TODO implement
    /**
     *
     * @param versionWithoutBuildNumber
     * @param refName
     * @return
     */
    boolean isPotentiallySameVersionIgnoringBuildNumber(String versionWithoutBuildNumber, String refName) {
        return true;
    }

}
