package com.github.danielflower.mavenplugins.release.pom;

import static com.github.danielflower.mavenplugins.release.pom.UpdateModel.ERROR_FORMAT;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.reactor.ReleasableModule;
import com.github.danielflower.mavenplugins.release.reactor.UnresolvedSnapshotDependencyException;

/**
 * @author rolandhauser
 *
 */
public class UpdateModelTest {
	private static final String GROUP_ID = "groupId";
	private static final String ARTIFACT_ID = "artifactId";
	private static final String VERSION = "version";
	private final MavenProject project = mock(MavenProject.class);
	private final Model model = mock(Model.class);
	private final Context context = mock(Context.class);
	private final ReleasableModule module = mock(ReleasableModule.class);
	private final UpdateModel update = new UpdateModel();

	@Before
	public void setup() throws Exception {
		when(project.getGroupId()).thenReturn(GROUP_ID);
		when(project.getArtifactId()).thenReturn(ARTIFACT_ID);
		when(project.getOriginalModel()).thenReturn(model);
		when(context.getProject()).thenReturn(project);
		when(context.getVersionToDependOn(GROUP_ID, ARTIFACT_ID)).thenReturn(module);
		when(module.getVersionToDependOn()).thenReturn(VERSION);
	}

	@Test
	public void verifyAlterModel() throws Exception {
		update.alterModel(context);
		verify(model).setVersion(VERSION);
	}

	@Test
	public void verifyAlterModelProjectNotFound() throws Exception {
		final UnresolvedSnapshotDependencyException expected = new UnresolvedSnapshotDependencyException(GROUP_ID,
				ARTIFACT_ID);
		doThrow(expected).when(context).getVersionToDependOn(GROUP_ID, ARTIFACT_ID);
		update.alterModel(context);
		verify(model, never()).setVersion(VERSION);
		verify(context).addError(ERROR_FORMAT, project);
	}

}
