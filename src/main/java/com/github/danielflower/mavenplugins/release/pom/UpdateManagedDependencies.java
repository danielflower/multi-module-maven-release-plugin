package com.github.danielflower.mavenplugins.release.pom;

import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.codehaus.plexus.component.annotations.Component;

/**
 * @author rolandhauser
 *
 */
@Component(role = Command.class, hint = "UpdateManagedDependencies")
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
