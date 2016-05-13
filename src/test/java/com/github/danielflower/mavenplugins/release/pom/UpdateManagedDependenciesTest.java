package com.github.danielflower.mavenplugins.release.pom;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.model.DependencyManagement;

public class UpdateManagedDependenciesTest extends UpdateDependenciesTest {
	private final DependencyManagement mgmt = mock(DependencyManagement.class);

	@Override
	protected UpdateManagedDependencies newCommand() {
		return new UpdateManagedDependencies();
	}

	@Override
	protected void setupDetermineDependencies() {
		when(mgmt.getDependencies()).thenReturn(dependencies);
		when(model.getDependencyManagement()).thenReturn(mgmt);
	}
}
