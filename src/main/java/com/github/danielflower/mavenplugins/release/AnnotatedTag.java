package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;

public class AnnotatedTag {
    public static final String VERSION = "version";
    public static final String BUILD_NUMBER = "buildNumber";
    private final String name;
    private final JSONObject message;

    private AnnotatedTag(String name, JSONObject message) {
        this.name = name;
        this.message = message;
    }

    public static AnnotatedTag create(String name, String version, String buildNumber) {
        JSONObject message = new JSONObject();
        message.put(VERSION, version);
        message.put(BUILD_NUMBER, buildNumber);
        return new AnnotatedTag(name, message);
    }

    public static AnnotatedTag fromRef(Repository repository, Ref gitTag) throws IOException {
        RevWalk walk = new RevWalk(repository);
        RevTag tag = walk.parseTag(gitTag.getObjectId());
        JSONObject message = (JSONObject) JSONValue.parse(tag.getFullMessage());
        return new AnnotatedTag(stripRefPrefix(gitTag.getName()), message);
    }

    static String stripRefPrefix(String refName) {
        return refName.substring("refs/tags/".length());
    }

    public String name() {
        return name;
    }

    public String version() {
        return String.valueOf(message.get(VERSION));
    }

    public String buildNumber() {
        return String.valueOf(message.get(BUILD_NUMBER));
    }

    public void saveAtHEAD(Git git) throws GitAPIException {
        String json = message.toJSONString();
        git.tag().setName(name()).setAnnotated(true).setMessage(json).call();
    }

    @Override
    public String toString() {
        return "AnnotatedTag{" +
            "name='" + name + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnotatedTag that = (AnnotatedTag) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
