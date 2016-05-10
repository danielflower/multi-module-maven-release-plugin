package com.github.danielflower.mavenplugins.release.pom;

import static com.github.danielflower.mavenplugins.release.pom.ValidateNoSnapshotPlugins.ERROR_FORMAT;
import static com.github.danielflower.mavenplugins.release.pom.ValidateNoSnapshotPlugins.MULTI_MODULE_MAVEN_PLUGIN_ARTIFACT_ID;
import static com.github.danielflower.mavenplugins.release.pom.ValidateNoSnapshotPlugins.MULTI_MODULE_MAVEN_PLUGIN_GROUP_ID;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

public class ValidateNoSnapshotPluginsTest {
	private static final String ANY_PROJECT_ARTIFACT_ID = "anyProjectArtifactId";
	private static final String ANY_ARTIFACT_ID = "anyArtifactId";
	private static final String ANY_GROUP_ID = "anyGroupId";
	private static final String ANY_VERSION = "anyVersion";
	private static final String ANY_SNAPSHOT_VERSION = "anyVersion-SNAPSHOT";
	private final Log log = mock(Log.class);
	private final Context context = mock(Context.class);
	private final MavenProject project = mock(MavenProject.class);
	private final Model model = mock(Model.class);
	private final Build build = mock(Build.class);
	private final Plugin plugin = mock(Plugin.class);
	private final List<Plugin> plugins = asList(plugin);
	private final ValidateNoSnapshotPlugins vld = new ValidateNoSnapshotPlugins();

	@Before
	public void setup() {
		vld.setCommand(log);
		when(context.getProject()).thenReturn(project);
		when(project.getModel()).thenReturn(model);
		when(model.getBuild()).thenReturn(build);
		when(build.getPlugins()).thenReturn(plugins);

		when(project.getArtifactId()).thenReturn(ANY_PROJECT_ARTIFACT_ID);
		when(plugin.getArtifactId()).thenReturn(ANY_ARTIFACT_ID);
		when(plugin.getGroupId()).thenReturn(ANY_GROUP_ID);
		when(plugin.getVersion()).thenReturn(ANY_VERSION);
	}

	@Test
	public void alterModelReleasedPlugin() {
		vld.alterModel(context);
		verify(context).getProject();
		verifyNoMoreInteractions(context);
	}

	@Test
	public void alterModelSnapshotPlugin() {
		when(plugin.getVersion()).thenReturn(ANY_SNAPSHOT_VERSION);
		vld.alterModel(context);
		verify(context).addError(ERROR_FORMAT, ANY_PROJECT_ARTIFACT_ID, ANY_ARTIFACT_ID, ANY_SNAPSHOT_VERSION);
	}

	@Test
	public void alterModelSnapshotPluginWithMultiReleaseGroupId() {
		when(plugin.getGroupId()).thenReturn(MULTI_MODULE_MAVEN_PLUGIN_GROUP_ID);
		when(plugin.getVersion()).thenReturn(ANY_SNAPSHOT_VERSION);
		vld.alterModel(context);
		verify(context).addError(ERROR_FORMAT, ANY_PROJECT_ARTIFACT_ID, ANY_ARTIFACT_ID, ANY_SNAPSHOT_VERSION);
	}

	@Test
	public void alterModelMultiModuleMavenPlugin() {
		when(plugin.getGroupId()).thenReturn(MULTI_MODULE_MAVEN_PLUGIN_GROUP_ID);
		when(plugin.getArtifactId()).thenReturn(MULTI_MODULE_MAVEN_PLUGIN_ARTIFACT_ID);
		vld.alterModel(context);
		verify(context).getProject();
		verifyNoMoreInteractions(context);
	}

	@Test
	public void alterModelSnapshotMultiModuleMavenPlugin() {
		when(plugin.getGroupId()).thenReturn(MULTI_MODULE_MAVEN_PLUGIN_GROUP_ID);
		when(plugin.getArtifactId()).thenReturn(MULTI_MODULE_MAVEN_PLUGIN_ARTIFACT_ID);
		when(plugin.getVersion()).thenReturn(ANY_SNAPSHOT_VERSION);
		vld.alterModel(context);
		verify(context).getProject();
		verifyNoMoreInteractions(context);
	}
}
