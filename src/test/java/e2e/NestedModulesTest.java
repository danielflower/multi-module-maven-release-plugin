package e2e;

import scaffolding.TestProject;

public class NestedModulesTest extends NestedModulesBaseTest {

	@Override
	protected TestProject newTestProject() {
		return TestProject.nestedProject();
	}

}
