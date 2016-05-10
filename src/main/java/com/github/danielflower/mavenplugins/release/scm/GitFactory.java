package com.github.danielflower.mavenplugins.release.scm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

import com.github.danielflower.mavenplugins.release.ValidationException;

@Component(role = GitFactory.class)
public class GitFactory {

	public Git newGit() throws ValidationException {
		final File gitDir = new File(".");
		try {
			return Git.open(gitDir);
		} catch (final RepositoryNotFoundException rnfe) {
			final String fullPathOfCurrentDir = pathOf(gitDir);
			final File gitRoot = getGitRootIfItExistsInOneOfTheParentDirectories(new File(fullPathOfCurrentDir));
			String summary;
			final List<String> messages = new ArrayList<String>();
			if (gitRoot == null) {
				summary = "Releases can only be performed from Git repositories.";
				messages.add(summary);
				messages.add(fullPathOfCurrentDir + " is not a Git repository.");
			} else {
				summary = "The release plugin can only be run from the root folder of your Git repository";
				messages.add(summary);
				messages.add(fullPathOfCurrentDir + " is not the root of a Gir repository");
				messages.add("Try running the release plugin from " + pathOf(gitRoot));
			}
			throw new ValidationException(summary, messages);
		} catch (final Exception e) {
			throw new ValidationException("Could not open git repository. Is " + pathOf(gitDir) + " a git repository?",
					Arrays.asList("Exception returned when accessing the git repo:", e.toString()));
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
