package de.hilling.maven.release;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import de.hilling.maven.release.releaseinfo.ReleaseInfoStorage;

public class TreeWalkingDiffDetector {

    private final Repository repo;

    public TreeWalkingDiffDetector(Repository repo) {
        this.repo = repo;
    }

    private static void stopWalkingWhenTheTagsAreHit(Ref tagReference, RevWalk walk) throws IOException {
        ObjectId commitId = tagReference.getTarget().getObjectId();
        RevCommit revCommit = walk.parseCommit(commitId);
        walk.markUninteresting(revCommit);
    }

    /**
     * Detect changes is given module
     *
     * @param moduleForChangeDetection module (directory) to run detection in.
     * @param childModules child modules (directories) to ignore.
     * @param tagReference reference tag to stop at.
     * @return true if any change was detected.
     * @throws IOException on git exception.
     */
    public boolean hasChangedSince(String moduleForChangeDetection, java.util.List<String> childModules, Ref tagReference) throws
                                                                                                             IOException {
        RevWalk walk = new RevWalk(repo);
        try {
            walk.setRetainBody(false);
            walk.markStart(walk.parseCommit(repo.getRef("HEAD").getObjectId()));
            filterOutOtherModulesChanges(moduleForChangeDetection, childModules, walk);
            stopWalkingWhenTheTagsAreHit(tagReference, walk);
            boolean changed = false;
            final Iterator<RevCommit> iterator = walk.iterator();
            while (iterator.hasNext()) {
                changed = true;
                System.out.println("change detected: " + iterator.next());
            }
            return changed;
        } finally {
            walk.dispose();
        }
    }

    private void filterOutOtherModulesChanges(String modulePath, List<String> childModules, RevWalk walk) {
        boolean isRootModule = ".".equals(modulePath);
        boolean isMultiModuleProject = !isRootModule || !childModules.isEmpty();
        List<TreeFilter> treeFilters = new ArrayList<>();
        treeFilters.add(TreeFilter.ANY_DIFF);
        if (isMultiModuleProject) {
            if (!isRootModule) {
                // for sub-modules, look for changes only in the sub-module path...
                treeFilters.add(PathFilter.create(modulePath));
            }

            // ... but ignore any sub-modules of the current sub-module, because they can change independently of the current module
            for (String childModule : childModules) {
                String path = isRootModule
                              ? childModule
                              : modulePath + "/" + childModule;
                treeFilters.add(PathFilter.create(path).negate());
            }
        }
        TreeFilter treeFilter = treeFilters.size() == 1
                                ? treeFilters.get(0)
                                : AndTreeFilter.create(treeFilters);
        TreeFilter releaseInfoFilter = new TreeFilter() {
            @Override
            public boolean include(TreeWalk walker) throws IOException {
                final String nameString = walker.getPathString();
                return !nameString.endsWith(ReleaseInfoStorage.RELEASE_INFO_FILE);
            }

            @Override
            public boolean shouldBeRecursive() {
                return true;
            }

            @Override
            public TreeFilter clone() {
                return this;
            }
        };
        walk.setTreeFilter(AndTreeFilter.create(releaseInfoFilter, treeFilter));
    }
}
