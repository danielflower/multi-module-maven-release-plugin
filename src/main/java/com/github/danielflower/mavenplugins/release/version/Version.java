package com.github.danielflower.mavenplugins.release.version;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

import com.github.danielflower.mavenplugins.release.scm.AnnotatedTag;

public interface Version {

	List<AnnotatedTag> getPreviousTagsForThisModule() throws MojoExecutionException;

	String releaseVersion();

	String businessVersion();

	long buildNumber();

	/**
	 * The snapshot version, e.g. "1.0-SNAPSHOT"
	 */
	String developmentVersion();

	AnnotatedTag hasChangedSinceLastRelease(String relativePathToModule) throws MojoExecutionException;
}
