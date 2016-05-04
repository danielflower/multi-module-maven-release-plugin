package com.github.danielflower.mavenplugins.release.pom;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
final class UpdateManagedDependencies extends UpdateDependencies {

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
