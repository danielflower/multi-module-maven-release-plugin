package com.github.danielflower.mavenplugins.release.scm;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.json.simple.JSONObject;

import com.github.danielflower.mavenplugins.release.Guard;

public class AnnotatedTag {
	public static final String VERSION = "version";
	public static final String BUILD_NUMBER = "buildNumber";
	private final String name;
	private final JSONObject message;
	private final Git git;
	private Ref ref;

	AnnotatedTag(final Git git, final Ref ref, final String name, final JSONObject message) {
		Guard.notBlank("tag name", name);
		Guard.notNull("tag message", message);
		this.git = git;
		this.ref = ref;
		this.name = name;
		this.message = message;
	}

	public String name() {
		return name;
	}

	public String version() {
		return String.valueOf(message.get(VERSION));
	}

	public long buildNumber() {
		return Long.parseLong(String.valueOf(message.get(BUILD_NUMBER)));
	}

	public Ref saveAtHEAD(final Git git) throws GitAPIException {
		final String json = message.toJSONString();
		ref = git.tag().setName(name()).setAnnotated(true).setMessage(json).call();
		return ref;
	}

	@Override
	public String toString() {
		return "AnnotatedTag{" + "name='" + name + '\'' + ", version=" + version() + ", buildNumber=" + buildNumber()
				+ '}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final AnnotatedTag that = (AnnotatedTag) o;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public Ref ref() {
		return ref;
	}
}
