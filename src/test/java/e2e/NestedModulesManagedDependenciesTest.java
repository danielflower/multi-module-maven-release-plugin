package e2e;

import scaffolding.TestProject;

public class NestedModulesManagedDependenciesTest extends NestedModulesBaseTest {

	@Override
	protected TestProject newTestProject() {
		return TestProject.nestedProjectManagedDependencies();
	}

}
