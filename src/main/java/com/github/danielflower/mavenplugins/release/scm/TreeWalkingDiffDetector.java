package com.github.danielflower.mavenplugins.release.scm;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TreeWalkingDiffDetector implements DiffDetector {

    private final Repository repo;

    public TreeWalkingDiffDetector(Repository repo) {
        this.repo = repo;
    }

    public boolean hasChangedSince(String modulePath, java.util.List<String> childModules, Collection<AnnotatedTag> tags) throws IOException {
        RevWalk walk = new RevWalk(repo);
        try {
            walk.setRetainBody(false);
            walk.markStart(walk.parseCommit(repo.getRef("HEAD").getObjectId()));
            filterOutOtherModulesChanges(modulePath, childModules, walk);
            stopWalkingWhenTheTagsAreHit(tags, walk);
            return walk.iterator().hasNext();
        } finally {
            walk.dispose();
        }
    }

    private static void stopWalkingWhenTheTagsAreHit(Collection<AnnotatedTag> tags, RevWalk walk) throws IOException {
        for (AnnotatedTag tag : tags) {
            ObjectId commitId = tag.ref().getTarget().getObjectId();
            RevCommit revCommit = walk.parseCommit(commitId);
            walk.markUninteresting(revCommit);
        }
    }

    private void filterOutOtherModulesChanges(String modulePath, List<String> childModules, RevWalk walk) {
        boolean isRootModule = ".".equals(modulePath);
        boolean isMultiModuleProject = !isRootModule || !childModules.isEmpty();
        List<TreeFilter> treeFilters = new ArrayList<TreeFilter>();
        treeFilters.add(TreeFilter.ANY_DIFF);
        if (isMultiModuleProject) {
            if (!isRootModule) {
                // for sub-modules, look for changes only in the sub-module path...
                treeFilters.add(PathFilter.create(modulePath));
            }

            // ... but ignore any sub-modules of the current sub-module, because they can change independently of the current module
            for (String childModule : childModules) {
                String path = isRootModule ? childModule : modulePath + "/" + childModule;
                treeFilters.add(PathFilter.create(path).negate());
            }

        }
        TreeFilter treeFilter = treeFilters.size() == 1 ? treeFilters.get(0) : AndTreeFilter.create(treeFilters);
        walk.setTreeFilter(treeFilter);
    }
}
