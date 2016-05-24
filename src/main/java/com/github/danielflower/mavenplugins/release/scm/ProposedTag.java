package com.github.danielflower.mavenplugins.release.scm;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;

public interface ProposedTag {

	String name();

	String version();

	long buildNumber();

	Ref saveAtHEAD() throws SCMException;

	void tagAndPush(String remoteUrl) throws SCMException;

	ObjectId getObjectId();
}
