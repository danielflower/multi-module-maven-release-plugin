package com.github.danielflower.mavenplugins.release.scm;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

@Component(role = GitFactory.class)
public class GitFactory {

	public Git newGit() throws SCMException {
		final File gitDir = new File(".");
		try {
			return Git.open(gitDir);
		} catch (final RepositoryNotFoundException rnfe) {
			final String fullPathOfCurrentDir = pathOf(gitDir);
			final File gitRoot = getGitRootIfItExistsInOneOfTheParentDirectories(new File(fullPathOfCurrentDir));
			if (gitRoot == null) {
				throw new SCMException("Releases can only be performed from Git repositories.")
						.add("%s is not a Git repository.", fullPathOfCurrentDir);
			}
			throw new SCMException("The release plugin can only be run from the root folder of your Git repository")
					.add("%s is not the root of a Gir repository", fullPathOfCurrentDir)
					.add("Try running the release plugin from %s", pathOf(gitRoot));
		} catch (final Exception e) {
			throw new SCMException("Could not open git repository. Is %s a git repository?", pathOf(gitDir))
					.add("Exception returned when accessing the git repo: %s", e.toString());
		}
	}

	private static String pathOf(final File file) {
		String path;
		try {
			path = file.getCanonicalPath();
		} catch (final IOException e1) {
			path = file.getAbsolutePath();
		}
		return path;
	}

	private static File getGitRootIfItExistsInOneOfTheParentDirectories(File candidateDir) {
		while (candidateDir != null && /* HACK ATTACK! Maybe.... */ !candidateDir.getName().equals("target")) {
			if (new File(candidateDir, ".git").isDirectory()) {
				return candidateDir;
			}
			candidateDir = candidateDir.getParentFile();
		}
		return null;
	}
}
