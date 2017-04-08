package de.hilling.maven.release;

import static de.hilling.maven.release.ReleaseMojo.getRemoteUrlOrNullIfNoneSet;
import static de.hilling.maven.release.repository.LocalGitRepo.fromCurrentDir;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.api.errors.GitAPIException;

import de.hilling.maven.release.releaseinfo.ReleaseInfoStorage;
import de.hilling.maven.release.repository.LocalGitRepo;
import de.hilling.maven.release.versioning.ReleaseInfo;

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

        final Scm originalScm = project.getOriginalModel().getScm();
        final Scm scm = project.getModel().getScm();
        LocalGitRepo repo = fromCurrentDir(getRemoteUrlOrNullIfNoneSet(originalScm, scm), getLog());
        ReleaseInfo previousRelease = new ReleaseInfoStorage(project.getBasedir(), repo.git).load();
        Reactor.fromProjects(getLog(), repo, project, projects, modulesToForceRelease, noChangesAction, bugfixRelease,
                             previousRelease);
    }
}
