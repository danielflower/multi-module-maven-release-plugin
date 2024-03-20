package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TreeWalkingDiffDetector implements DiffDetector {

    private final Repository repo;
    private final Set<String> ignoredPaths;

    TreeWalkingDiffDetector(Repository repo, Set<String> ignoredPaths) {
        this.repo = repo;
        this.ignoredPaths = ignoredPaths;
    }

    TreeWalkingDiffDetector(Repository repository) {
        this(repository, Collections.emptySet());
    }

    public boolean hasChangedSince(String modulePath, List<String> childModules, Collection<AnnotatedTag> tags) throws IOException {
        RevWalk walk = new RevWalk(repo);
        try {
            walk.setRetainBody(false);
            walk.markStart(walk.parseCommit(repo.getRefDatabase().findRef("HEAD").getObjectId()));

            List<TreeFilter> treeFilters = createTreeFiltersForOtherModulesChanges(modulePath, childModules);
            treeFilters.addAll(createTreeFiltersForIgnoredPaths());
            walk.setTreeFilter(treeFilters.size() == 1 ? treeFilters.get(0) : AndTreeFilter.create(treeFilters));
            stopWalkingWhenTheTagsAreHit(tags, walk);

            return walk.iterator().hasNext();
        } finally {
            walk.dispose();
        }
    }

    private Collection<TreeFilter> createTreeFiltersForIgnoredPaths() {
        List<TreeFilter> treeFilters = new ArrayList<>();
        if (ignoredPaths != null) {
            treeFilters.addAll(
                ignoredPaths.stream()
                    // To differentiate path suffix filters from path filters in the configuration there is the special
                    // "**" prefix.
                    // foo.txt -> path filter that matches foo.txt in the root of the top-level project
                    // bar/foo.txt -> path filter that matches foo.txt in the root of the bar directory
                    // bar -> path filter that matches everything in/under the bar directory
                    // **.txt -> path suffix filter that matches all paths ending in .txt (suffix match)
                    // **.editorconfig -> path suffix filter that matches all .editorconfig files in all (sub)directories; a special case of suffix matching
                    .map(p -> p.startsWith("**") ? PathSuffixFilter.create(p.substring(2)): PathFilter.create(p))
                    // tree filters define what to include, yet the users define what to IGNORE -> negate the filter
                    .map(TreeFilter::negate)
                    .collect(Collectors.toList())
            );
        }
        return treeFilters;
    }

    private static void stopWalkingWhenTheTagsAreHit(Collection<AnnotatedTag> tags, RevWalk walk) throws IOException {
        for (AnnotatedTag tag : tags) {
            ObjectId commitId = tag.ref().getTarget().getObjectId();
            RevCommit revCommit = walk.parseCommit(commitId);
            walk.markUninteresting(revCommit);
        }
    }

    private List<TreeFilter> createTreeFiltersForOtherModulesChanges(String modulePath, List<String> childModules) {
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
                String path = isRootModule ? childModule : modulePath + "/" + childModule;
                treeFilters.add(PathFilter.create(path).negate());
            }

        }
        return treeFilters;
    }
}
