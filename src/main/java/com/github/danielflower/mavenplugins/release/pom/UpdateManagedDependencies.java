package com.github.danielflower.mavenplugins.release.pom;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
final class UpdateManagedDependencies extends UpdateDependencies {

	@com.google.inject.Inject // Compatibility: Maven 3.0.1 - 3.2.1
	@Inject // Maven 3.3.0 and greater
	UpdateManagedDependencies(final Log log) {
		super(log);
	}

	@Override
	protected List<Dependency> determineDependencies(final Model originalModel) {
		List<Dependency> dependencies = Collections.emptyList();
		final DependencyManagement mgmt = originalModel.getDependencyManagement();

		if (mgmt != null) {
			dependencies = mgmt.getDependencies();
		}
		return dependencies;
	}
}
