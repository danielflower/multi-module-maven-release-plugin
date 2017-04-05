package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.releaseinfo.ReleaseInfoStorage;
import com.github.danielflower.mavenplugins.release.repository.LocalGitRepo;
import com.github.danielflower.mavenplugins.release.versioning.ReleaseInfo;

/**
 * Logs the versions of the modules that the releaser will release on the next release. Does not run the build nor
 * tag the repo.
 *
 * @since 1.4.0
 */
@Mojo(name = "next", requiresDirectInvocation = true,
      // this should not be bound to a phase as this plugin starts a phase itself
      inheritByDefault = true, // so you can configure this in a shared parent pom
      requiresProject = true, // this can only run against a maven project
      aggregator = true // the plugin should only run once against the aggregator pom
      )
public class NextMojo extends BaseMojo {

    @Override
    public void executeConcreteMojo() throws MojoExecutionException, MojoFailureException, GitAPIException {
        configureJsch();

        LocalGitRepo repo = LocalGitRepo.fromCurrentDir(
            ReleaseMojo.getRemoteUrlOrNullIfNoneSet(project.getOriginalModel().getScm(), project.getModel().getScm()));
        ReleaseInfo previousRelease = new ReleaseInfoStorage(project.getBasedir(), repo.git).load();
        Reactor.fromProjects(getLog(), repo, project, projects, modulesToForceRelease, noChangesAction, bugfixRelease,
                             previousRelease);
    }
}
