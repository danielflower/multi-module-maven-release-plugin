package com.github.danielflower.mavenplugins.release;

import static java.util.stream.Collectors.groupingBy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.utils.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

class AnnotatedTagFinder {
    private VersionNamer versionNamer;

    AnnotatedTagFinder(VersionNamer versionNamer) {
        this.versionNamer = versionNamer;
    }

    List<AnnotatedTag> tagsForVersion(Git git, String groupId, String artifactId, String versionWithoutBuildNumber, String tagNameFormat, Log log) throws MojoExecutionException {
        ArrayList<AnnotatedTag> results = new ArrayList<AnnotatedTag>();
        try {
            ArrayList<RefWithCommitId> tagCommits = getAnnotatedTags(git, groupId, artifactId, versionWithoutBuildNumber, tagNameFormat, log);
            Map<ObjectId, List<RefWithCommitId>> commitTags = tagCommits.stream().collect(groupingBy(
                RefWithCommitId::getCommitObjectId));
            Iterable<RevCommit> commits = git.log().call();
            for (RevCommit commit : commits) {
                if (commitTags.containsKey(commit.getId())) {
                    for (RefWithCommitId tag : commitTags.get(commit.getId())) {
                            try {
                                results.add(AnnotatedTag.fromRefCommit(git.getRepository(), tag.getRef(), commit));
                            } catch (IncorrectObjectTypeException ignored) {
                                // not actually a tag, so skip it.
                            } catch (IOException e) {
                                throw new MojoExecutionException("Error while looking up tag " + tag, e);
                            }
                    }
                    // we want the version of the latest commit so we stop here
                    break;
                }
            }
        } catch (GitAPIException | IOException e) {
            throw new MojoExecutionException("Error while getting a list of annotated tags in the local repo", e);
        }
        return results;
    }

    private ArrayList<RefWithCommitId> getAnnotatedTags(Git git, String groupId, String artifactId, String versionWithoutBuildNumber, String tagNameFormat, Log log)
        throws GitAPIException, IOException
    {
        String tagWithoutBuildNumber = artifactId + "-" + versionWithoutBuildNumber;
        if (StringUtils.isNotEmpty(tagNameFormat)) {
            tagWithoutBuildNumber = AnnotatedTag.formatTagName(tagWithoutBuildNumber, groupId, artifactId, versionWithoutBuildNumber, tagNameFormat, log);
        }
        ArrayList<RefWithCommitId> allTags = new ArrayList<RefWithCommitId>();
        List<Ref> tags = git.tagList().call();

        for (Ref ref : tags) {
            if(!isPotentiallySameVersionIgnoringBuildNumber(tagWithoutBuildNumber, ref.getName())) continue;

            LogCommand gitLog = git.log();

            Ref peeledRef = git.getRepository().getRefDatabase().peel(ref);
            if(peeledRef.getPeeledObjectId() != null) {
                gitLog.add(peeledRef.getPeeledObjectId());
            } else {
                gitLog.add(ref.getObjectId());
            }
            RevCommit commit = gitLog.call().iterator().next();
            allTags.add(new RefWithCommitId(ref, commit.getId()));
        }
        return allTags;
    }

    boolean isPotentiallySameVersionIgnoringBuildNumber(String versionWithoutBuildNumber, String refName) {
        return buildNumberOf(versionWithoutBuildNumber, refName) != null;
    }

    Long buildNumberOf(String versionWithoutBuildNumber, String refName) {
        String tagName = AnnotatedTag.stripRefPrefix(refName);
        String prefix = versionWithoutBuildNumber + versionNamer.getDelimiter();
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
