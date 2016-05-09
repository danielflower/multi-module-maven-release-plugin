package com.github.danielflower.mavenplugins.release.pom;

import static com.github.danielflower.mavenplugins.release.pom.UpdateDependencies.ERROR_FORMAT;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.UnresolvedSnapshotDependencyException;
import com.github.danielflower.mavenplugins.release.reactor.Reactor;

public class UpdateDependenciesTest {
	private static final String ANY_GROUP_ID = "anyGroupId";
	private static final String ANY_ARTIFACT_ID = "anyArtifactId";
	private static final String ANY_VERSION = "anyVersion";
	private static final String ANY_SNAPSHOT_VERSION = ANY_VERSION + "-SNAPSHOT";
	private final Reactor reactor = mock(Reactor.class);
	private final ReleasableModule module = mock(ReleasableModule.class);
	protected final Log log = mock(Log.class);
	private final Context context = mock(Context.class);
	private final MavenProject project = mock(MavenProject.class);
	protected final Model model = mock(Model.class);
	private final Dependency dependency = mock(Dependency.class);
	protected final List<Dependency> dependencies = asList(dependency);
	private final Command cmd = newCommand();

	protected Command newCommand() {
		return new UpdateDependencies(log);
	}

	@Before
	public void setup() throws Exception {
		setupDetermineDependencies();

		// Setup reactor
		when(reactor.find(ANY_GROUP_ID, ANY_ARTIFACT_ID, ANY_SNAPSHOT_VERSION)).thenReturn(module);
		when(module.getVersionToDependOn()).thenReturn(ANY_VERSION);

		// Setup project
		when(context.getProject()).thenReturn(project);
		when(project.getArtifactId()).thenReturn(ANY_ARTIFACT_ID);
		when(context.getReactor()).thenReturn(reactor);
		when(project.getOriginalModel()).thenReturn(model);

		// Setup dependency
		when(dependency.getGroupId()).thenReturn(ANY_GROUP_ID);
		when(dependency.getArtifactId()).thenReturn(ANY_ARTIFACT_ID);
		when(dependency.getVersion()).thenReturn(ANY_SNAPSHOT_VERSION);

	}

	protected void setupDetermineDependencies() {
		when(model.getDependencies()).thenReturn(dependencies);
	}

	@Test
	public void alterModelSnapshotDependencyUpdated() {
		cmd.alterModel(context);
		final InOrder order = inOrder(dependency, log);
		order.verify(dependency).setVersion(ANY_VERSION);
		order.verify(log).debug(" Dependency on anyArtifactId rewritten to version anyVersion");
	}

	@Test
	public void alterModelReleaseDependencyKept() {
		when(dependency.getVersion()).thenReturn(ANY_VERSION);
		cmd.alterModel(context);
		final InOrder order = inOrder(dependency, log);
		verify(dependency, never()).setVersion(ANY_VERSION);
		order.verify(log).debug(" Dependency on anyArtifactId kept at version anyVersion");
	}

	@Test
	public void exceptionOccurred() throws Exception {
		final UnresolvedSnapshotDependencyException expected = new UnresolvedSnapshotDependencyException(ANY_GROUP_ID,
				ANY_ARTIFACT_ID, ANY_SNAPSHOT_VERSION);
		doThrow(expected).when(reactor).find(ANY_GROUP_ID, ANY_ARTIFACT_ID, ANY_SNAPSHOT_VERSION);
		cmd.alterModel(context);
		verify(dependency, never()).setVersion(ANY_VERSION);
		verify(context).addError(ERROR_FORMAT, ANY_ARTIFACT_ID, ANY_ARTIFACT_ID, ANY_SNAPSHOT_VERSION);
	}
}
