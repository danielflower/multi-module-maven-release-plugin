package com.github.danielflower.mavenplugins.release.scm;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;

import com.github.danielflower.mavenplugins.release.version.Version;

public interface ProposedTag extends Version {

	String name();

	Ref saveAtHEAD() throws SCMException;

	void tagAndPush(String remoteUrl) throws SCMException;

	ObjectId getObjectId();
}
