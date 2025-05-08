package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TreeWalkingDiffDetector implements DiffDetector {

    private final Repository repo;
    private final Set<String> ignoredPaths;
    private final Set<String> requiredPaths;

    public TreeWalkingDiffDetector(Repository repo, Set<String> ignoredPaths, Set<String> requiredPaths) {
        this.repo = repo;
        this.ignoredPaths = ignoredPaths != null ? ignoredPaths : new HashSet<>();
        this.requiredPaths = requiredPaths;
    }

    public boolean hasChangedSince(String modulePath, java.util.List<String> childModules, Collection<AnnotatedTag> tags) throws IOException {
        RevWalk walk = new RevWalk(repo);
        try {
            walk.setRetainBody(false);
            walk.markStart(walk.parseCommit(repo.getRefDatabase().findRef("HEAD").getObjectId()));
            List<TreeFilter> treeFilters = new ArrayList<>();
            filterOutOtherModulesChanges(modulePath, childModules, treeFilters);
            if (requiredPaths == null || requiredPaths.isEmpty()) {
                filterOutIgnoredPathsChanges(treeFilters);
            } else {
                filterRequiredPathsChanges(treeFilters);
            }
            walk.setTreeFilter(treeFilters.size() == 1 ? treeFilters.get(0) : AndTreeFilter.create(treeFilters));
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

    private void filterOutOtherModulesChanges(String modulePath, List<String> childModules, List<TreeFilter> treeFilters) {
        boolean isRootModule = ".".equals(modulePath);
        boolean isMultiModuleProject = !isRootModule || !childModules.isEmpty();
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
    }

    private void filterOutIgnoredPathsChanges(List<TreeFilter> treeFilters) {
        for (String ignoredPath : ignoredPaths) {
            TreeFilter filter;
            if (ignoredPath.startsWith("/")) {
                filter = PathFilter.create(ignoredPath.substring(1));
            } else {
                filter = PathSuffixFilter.create(ignoredPath);
            }
            treeFilters.add(filter.negate());
        }
    }

    private void filterRequiredPathsChanges(List<TreeFilter> treeFilters) {
        List<TreeFilter> filters = requiredPaths.stream()
            .filter(requiredPath -> !requiredPath.isEmpty())
            .map(requiredPath -> {
                if (requiredPath.startsWith("/")) {
                    return PathFilter.create(requiredPath.substring(1));
                } else {
                    return PathSuffixFilter.create(requiredPath);
                }
            })
            .collect(Collectors.toList());
        if (filters.isEmpty()) {
            return;
        }
        treeFilters.add(filters.size() == 1 ? filters.get(0) : OrTreeFilter.create(filters));
    }
}
