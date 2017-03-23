package com.github.danielflower.mavenplugins.release;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
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

    boolean isPotentiallySameVersionIgnoringBuildNumber(String versionWithoutBuildNumber, String refName) {
        return buildNumberOf(versionWithoutBuildNumber, refName).getBuildNumber() != null;
    }

    public VersionInfo buildNumberOf(String versionWithoutBuildNumber, String refName) {
        String tagName = AnnotatedTag.stripRefPrefix(refName);
        String prefix;
        if (findBugfixReleases) {
            prefix = versionWithoutBuildNumber.replaceFirst("^([^.]+).*$", "$1");
        } else {
            prefix = versionWithoutBuildNumber;
        }
        final int beginIndex = prefix.length() + 1;
        if (tagName.length() > beginIndex) {
            String versionNumber = tagName.substring(beginIndex);

            if (tagName.startsWith(prefix)) {
                if (findBugfixReleases) {
                    final Matcher matcher = SINGLE_OR_MAJOR_MINOR_VERSION.matcher(versionNumber);
                    if (matcher.matches()) {
                        final long firstNumber = Long.parseLong(matcher.group("first"));
                        final String secondGroup = matcher.group("second");
                        if (secondGroup == null) {
                            return new VersionInfo(null, firstNumber);
                        } else {
                            final long secondNumber = Long.parseLong(secondGroup);
                            return new VersionInfo(firstNumber, secondNumber);
                        }
                    }
                } else {
                    if (SINGLE_NUMBER.matcher(versionNumber).matches()) {
                        return new VersionInfo(null, Long.parseLong(versionNumber));
                    }
                }
            }
        }
        return new VersionInfo(null, null);
    }
}
