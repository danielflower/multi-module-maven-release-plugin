package com.github.danielflower.mavenplugins.release.scm;

import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.ValidationException;
import com.google.inject.Provider;

@Named
@Singleton
final class SCMRepositoryProvider implements Provider<SCMRepository> {
	static final String ERROR_SUMMARY = "Cannot run the release plugin with a non-Git version control system";
	static final String GIT_PREFIX = "scm:git:";
	private final SCMRepository repository;

	@com.google.inject.Inject // Compatibility: Maven 3.0.1 - 3.2.1
	@Inject // Maven 3.3.0 and greater
	SCMRepositoryProvider(final Log log, final GitFactory factory, final MavenProject project) {
		SCMRepository repository;
		try {
			repository = new GitRepository(log, factory.newGit(), getRemoteUrlOrNullIfNoneSet(project.getScm()));
		} catch (final ValidationException e) {
			repository = (SCMRepository) newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] { SCMRepository.class }, new InstantiationFailedHandler(e));
		}
		this.repository = repository;
	}

	static String getRemoteUrlOrNullIfNoneSet(final Scm scm) throws ValidationException {
		String remote = null;
		if (scm != null) {
			remote = scm.getDeveloperConnection();
			if (remote == null) {
				remote = scm.getConnection();
			}
			if (remote != null) {
				if (!remote.startsWith(GIT_PREFIX)) {
					final List<String> messages = new ArrayList<String>();
					messages.add(ERROR_SUMMARY);
					messages.add(format("The value in your scm tag is %s", remote));
					throw new ValidationException(format("%s %s", ERROR_SUMMARY, remote), messages);
				}
				remote = remote.substring(GIT_PREFIX.length()).replace("file://localhost/", "file:///");
			}
		}
		return remote;
	}

	@Override
	public SCMRepository get() {
		return repository;
	}
}
