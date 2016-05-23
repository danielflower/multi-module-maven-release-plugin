package com.github.danielflower.mavenplugins.release.pom;

import static com.github.danielflower.mavenplugins.release.pom.UpdateParent.ERROR_FORMAT;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;
import com.github.danielflower.mavenplugins.release.reactor.ReleasableModule;
import com.github.danielflower.mavenplugins.release.reactor.UnresolvedSnapshotDependencyException;

public class UpdateParentTest {
	private static final String ANY_ARTIFACT_ID = "anyArtifactId";
	private static final String ANY_PARENT_GROUP_ID = "anyParentGroupId";
	private static final String ANY_PARENT_ARTIFACT_ID = "anyParentArtifactId";
	private static final String ANY_PARENT_VERSION = "anyVersion";
	private static final String ANY_PARENT_SNAPSHOT_VERSION = "anyVersion-SNAPSHOT";
	private final Log log = mock(Log.class);
	private final MavenProject project = mock(MavenProject.class);
	private final Model originalModel = mock(Model.class);
	private final Parent parent = mock(Parent.class);
	private final MavenProject parentProject = mock(MavenProject.class);
	private final Context context = mock(Context.class);
	private final Reactor reactor = mock(Reactor.class);
	private final ReleasableModule module = mock(ReleasableModule.class);
	private final UpdateParent cmd = new UpdateParent();

	@Before
	public void setup() throws Exception {
		cmd.setCommand(log);
		when(context.getProject()).thenReturn(project);
		when(project.getArtifactId()).thenReturn(ANY_ARTIFACT_ID);
		when(project.getParent()).thenReturn(parentProject);
		when(project.getOriginalModel()).thenReturn(originalModel);
		when(originalModel.getParent()).thenReturn(parent);

		when(parentProject.getArtifactId()).thenReturn(ANY_PARENT_ARTIFACT_ID);
		when(parentProject.getGroupId()).thenReturn(ANY_PARENT_GROUP_ID);
		when(parentProject.getVersion()).thenReturn(ANY_PARENT_SNAPSHOT_VERSION);
		when(module.getVersionToDependOn()).thenReturn(ANY_PARENT_VERSION);

		when(context.getReactor()).thenReturn(reactor);
		when(reactor.find(ANY_PARENT_GROUP_ID, ANY_PARENT_ARTIFACT_ID, ANY_PARENT_SNAPSHOT_VERSION)).thenReturn(module);
	}

	@Test
	public void alterModelNoParentDeclared() {
		when(parentProject.getVersion()).thenReturn(ANY_PARENT_VERSION);
		cmd.alterModel(context);
		verifyZeroInteractions(parent, log);
	}

	@Test
	public void alterModelParentIsReleased() {
		when(project.getParent()).thenReturn(null);
		cmd.alterModel(context);
		verifyZeroInteractions(log, parent);
	}

	@Test
	public void alterModelParentUpdated() {
		cmd.alterModel(context);
		final InOrder order = inOrder(parent, log);
		order.verify(parent).setVersion(ANY_PARENT_VERSION);
		order.verify(log).debug(" Parent anyParentArtifactId rewritten to version anyVersion");
	}

	@Test
	public void exceptionOccurred() throws Exception {
		final UnresolvedSnapshotDependencyException expected = new UnresolvedSnapshotDependencyException(
				ANY_PARENT_GROUP_ID, ANY_PARENT_ARTIFACT_ID, ANY_PARENT_SNAPSHOT_VERSION);
		doThrow(expected).when(reactor).find(ANY_PARENT_GROUP_ID, ANY_PARENT_ARTIFACT_ID, ANY_PARENT_SNAPSHOT_VERSION);
		cmd.alterModel(context);
		verify(parent, never()).setVersion(ANY_PARENT_VERSION);
		verify(context).addError(ERROR_FORMAT, ANY_ARTIFACT_ID, ANY_PARENT_ARTIFACT_ID, ANY_PARENT_SNAPSHOT_VERSION);
	}
}
