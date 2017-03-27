package com.github.danielflower.mavenplugins.release;

import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;

import com.github.danielflower.mavenplugins.release.versioning.ImmutableReleaseInfo;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AnnotatedTag {

    private final String name;

    private final ReleaseInfo releaseInfo;
    private       Ref         ref;

    public AnnotatedTag(Ref ref, String name, ReleaseInfo releaseInfo) {
        Guard.notBlank("tag name", name);
        Guard.notNull("tag message", releaseInfo);
        this.ref = ref;
        this.name = name;
        this.releaseInfo = releaseInfo;
    }

    public static AnnotatedTag fromRef(Repository repository, Ref gitTag) throws IOException,
                                                                                 IncorrectObjectTypeException {
        Guard.notNull("gitTag", gitTag);

        RevWalk walk = new RevWalk(repository);
        ImmutableReleaseInfo releaseInfo;
        try {
            ObjectId tagId = gitTag.getObjectId();
            RevTag tag = walk.parseTag(tagId);
            releaseInfo = new Gson().fromJson(tag.getFullMessage(), ImmutableReleaseInfo.class);
        } finally {
            walk.dispose();
        }
        return new AnnotatedTag(gitTag, stripRefPrefix(gitTag.getName()), releaseInfo);
    }

    static String stripRefPrefix(String refName) {
        return refName.substring("refs/tags/".length());
    }

    public Ref saveAtHEAD(Git git) throws GitAPIException {
        final String message = new GsonBuilder().setPrettyPrinting().create().toJson(releaseInfo);
        ref = git.tag().setName(name).setAnnotated(true).setMessage(message).call();
        return ref;
    }

    public Ref ref() {
        return ref;
    }

    public ReleaseInfo getReleaseInfo() {
        return releaseInfo;
    }

    public String name() {
        return name;
    }
}
