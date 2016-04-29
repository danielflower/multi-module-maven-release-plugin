package com.github.danielflower.mavenplugins.release.scm;

import static java.lang.reflect.Proxy.newProxyInstance;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.project.MavenProject;

import com.google.inject.Provider;

@Named
@Singleton
final class SCMRepositoryFactory implements Provider<SCMRepository> {
	private final SCMRepository repository;

	@Inject
	SCMRepositoryFactory(final MavenProject project) {
		SCMRepository repository;
		try {
			repository = new GitRepository(project);
		} catch (final RuntimeException e) {
			repository = (SCMRepository) newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] { SCMRepository.class }, new InstantiationFailedHandler(e));
		}
		this.repository = repository;
	}

	@Override
	public SCMRepository get() {
		return repository;
	}

}
