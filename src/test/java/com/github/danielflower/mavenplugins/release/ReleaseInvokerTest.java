package com.github.danielflower.mavenplugins.release;

import static com.github.danielflower.mavenplugins.release.ReleaseInvoker.DEPLOY;
import static com.github.danielflower.mavenplugins.release.ReleaseInvoker.SKIP_TESTS;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;

/**
 * @author Roland Hauser sourcepond@gmail.com
 *
 */
public class ReleaseInvokerTest {
	private final static String ACTIVE_PROFILE_ID = "activeProfile";
	private final static String SOME_PROFILE_ID = "someProfile";
	private final static File GLOBAL_SETTINGS = new File("file:///globalSettings");
	private final static File USER_SETTINGS = new File("file:///globalSettings");
	private final static String MODULE_PATH = "modulePath";
	private final static String SITE = "site";
	private final Log log = mock(Log.class);
	private final MavenProject project = mock(MavenProject.class);
	private final InvocationRequest request = mock(InvocationRequest.class);
	private final InvocationResult result = mock(InvocationResult.class);
	private final Invoker invoker = mock(Invoker.class);
	private final List<String> goals = new LinkedList<String>();
	private final List<String> modulesToRelease = new LinkedList<String>();
	private final List<String> releaseProfiles = new LinkedList<String>();
	@SuppressWarnings("unchecked")
	private final Iterator<ReleasableModule> modulesInBuildOrder = mock(Iterator.class);
	private final Reactor reactor = mock(Reactor.class);
	private final ReleasableModule module = mock(ReleasableModule.class);
	private final Profile activeProfile = mock(Profile.class);
	private final ReleaseInvoker releaseInvoker = new ReleaseInvoker(log, project, request, invoker);

	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws Exception {
		when(modulesInBuildOrder.next()).thenReturn(module).thenThrow(NoSuchElementException.class);
		when(modulesInBuildOrder.hasNext()).thenReturn(true).thenReturn(false);
		when(reactor.iterator()).thenReturn(modulesInBuildOrder);
		when(log.isDebugEnabled()).thenReturn(true);
		when(invoker.execute(request)).thenReturn(result);
		when(activeProfile.getId()).thenReturn(ACTIVE_PROFILE_ID);
		when(module.getRelativePathToModule()).thenReturn(MODULE_PATH);
	}

	@Test
	public void verifyDefaultConstructor() {
		new ReleaseInvoker(log, project);
	}

	@Test
	public void runMavenBuild_BaseTest() throws Exception {
		releaseInvoker.runMavenBuild(reactor);
		verify(request).setInteractive(false);
		verify(request).setShowErrors(true);
		verify(request).setDebug(true);
		verify(log).isDebugEnabled();
		verify(request).setAlsoMake(true);
		verify(request).setGoals(Mockito.argThat(new ArgumentMatcher<List<String>>() {

			@Override
			public boolean matches(final Object item) {
				@SuppressWarnings("unchecked")
				final List<String> goals = (List<String>) item;
				return goals.size() == 1 && goals.contains(DEPLOY);
			}

			@Override
			public String toString() {
				return "deploy";
			}
		}));
		verify(request).setProjects(Mockito.argThat(new ArgumentMatcher<List<String>>() {

			@Override
			@SuppressWarnings("unchecked")
			public boolean matches(final Object item) {
				return ((List<String>) item).isEmpty();
			}

			@Override
			public String toString() {
				return "projects";
			}
		}));
		verify(log).info("About to run mvn [deploy] with no profiles activated");
	}

	@Test
	public void runMavenBuild_WithUserSettings() throws Exception {
		releaseInvoker.setUserSettings(USER_SETTINGS);
		releaseInvoker.runMavenBuild(reactor);
		verify(request).setUserSettingsFile(USER_SETTINGS);
	}

	@Test
	public void runMavenBuild_WithGlobalSettings() throws Exception {
		releaseInvoker.setGlobalSettings(GLOBAL_SETTINGS);
		releaseInvoker.runMavenBuild(reactor);
		verify(request).setGlobalSettingsFile(GLOBAL_SETTINGS);
	}

	@Test
	public void runMavenBuild_WithReleasableModule() throws Exception {
		// releaseProfiles.add(e)
	}

	@Test
	public void runMavenBuild_WithGoals() throws Exception {
		goals.add(SITE);
		releaseInvoker.setGoals(goals);
		releaseInvoker.runMavenBuild(reactor);
		verify(request).setGoals(Mockito.argThat(new ArgumentMatcher<List<String>>() {

			@Override
			@SuppressWarnings("unchecked")
			public boolean matches(final Object item) {
				final List<String> goals = (List<String>) item;
				return goals.size() == 1 && goals.contains(SITE);
			}

			@Override
			public String toString() {
				return "goals";
			}
		}));
	}

	@Test
	public void runMavenBuild_WithActiveProfiles() throws Exception {
		releaseProfiles.add(SOME_PROFILE_ID);
		releaseInvoker.setReleaseProfiles(releaseProfiles);
		when(project.getActiveProfiles()).thenReturn(asList(activeProfile));
		releaseInvoker.runMavenBuild(reactor);
		verify(request).setProfiles(Mockito.argThat(new ArgumentMatcher<List<String>>() {

			@Override
			@SuppressWarnings("unchecked")
			public boolean matches(final Object item) {
				final List<String> profiles = (List<String>) item;
				return profiles.size() == 2 && profiles.contains(ACTIVE_PROFILE_ID)
						&& profiles.contains(SOME_PROFILE_ID);
			}

			@Override
			public String toString() {
				return "profiles";
			}
		}));
	}

	@Test
	public void runMavenBuild_UserExplicitlyWantsThisToBeReleased() throws Exception {
		modulesToRelease.add(MODULE_PATH);
		releaseInvoker.setModulesToRelease(modulesToRelease);
		releaseInvoker.runMavenBuild(reactor);
		verify(request).setProjects(Mockito.argThat(new ArgumentMatcher<List<String>>() {

			@Override
			@SuppressWarnings("unchecked")
			public boolean matches(final Object item) {
				final List<String> modules = (List<String>) item;
				return modules.size() == 1 && modules.contains(MODULE_PATH);
			}

			@Override
			public String toString() {
				return "projects";
			}
		}));
	}

	@Test
	public void runMavenBuild_UserImplicitlyWantsThisToBeReleased() throws Exception {
		when(module.willBeReleased()).thenReturn(true);
		releaseInvoker.setModulesToRelease(modulesToRelease);
		releaseInvoker.runMavenBuild(reactor);
		verify(request).setProjects(Mockito.argThat(new ArgumentMatcher<List<String>>() {

			@Override
			@SuppressWarnings("unchecked")
			public boolean matches(final Object item) {
				final List<String> modules = (List<String>) item;
				return modules.size() == 1 && modules.contains(MODULE_PATH);
			}

			@Override
			public String toString() {
				return "projects";
			}
		}));
	}

	@Test
	public void runMavenBuild_UserImplicitlyWantsThisToBeReleased_WillNotBeReleased() throws Exception {
		releaseInvoker.setModulesToRelease(modulesToRelease);
		releaseInvoker.runMavenBuild(reactor);
		verify(request).setProjects(Mockito.argThat(new ArgumentMatcher<List<String>>() {

			@Override
			@SuppressWarnings("unchecked")
			public boolean matches(final Object item) {
				return ((List<String>) item).isEmpty();
			}

			@Override
			public String toString() {
				return "projects";
			}
		}));
	}

	@Test
	public void skipTests() throws Exception {
		releaseInvoker.setSkipTests(true);
		releaseInvoker.runMavenBuild(reactor);
		verify(request).setGoals(Mockito.argThat(new ArgumentMatcher<List<String>>() {

			@Override
			@SuppressWarnings("unchecked")
			public boolean matches(final Object item) {
				final List<String> goals = (List<String>) item;
				return goals.size() == 2 && goals.contains(DEPLOY) && goals.contains(SKIP_TESTS);
			}

			@Override
			public String toString() {
				return "goals";
			}
		}));
	}

	@Test(expected = MojoExecutionException.class)
	public void runMavenBuild_ErrorExitCode() throws Exception {
		when(result.getExitCode()).thenReturn(1);
		releaseInvoker.runMavenBuild(reactor);
	}

	@Test
	public void runMavenBuild_InvocationFailed() throws Exception {
		final MavenInvocationException expected = new MavenInvocationException("anyMessage");
		doThrow(expected).when(invoker).execute(request);
		try {
			releaseInvoker.runMavenBuild(reactor);
			fail("Exception expected here");
		} catch (final MojoExecutionException e) {
			assertSame(expected, e.getCause());
		}
	}
}
