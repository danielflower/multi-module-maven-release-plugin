package com.github.danielflower.mavenplugins.release.scm;

import org.eclipse.jgit.lib.Ref;

public interface ProposedTag {

	String name();

	String version();

	long buildNumber();

	Ref ref();

	Ref saveAtHEAD() throws SCMException;

}
