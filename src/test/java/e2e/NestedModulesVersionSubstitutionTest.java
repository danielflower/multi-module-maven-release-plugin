package e2e;

import scaffolding.TestProject;

public class NestedModulesVersionSubstitutionTest extends NestedModulesBaseTest {

	@Override
	protected TestProject newTestProject() throws Exception {
		final TestProject project = TestProject.nestedProjectVersionSubstitution();
		project.mvn("install");
		return project;
	}

}