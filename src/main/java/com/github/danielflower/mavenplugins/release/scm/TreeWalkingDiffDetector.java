package com.github.danielflower.mavenplugins.release.scm;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

public class TreeWalkingDiffDetector implements DiffDetector {

	private final Repository repo;

	public TreeWalkingDiffDetector(final Repository repo) {
		this.repo = repo;
	}

	@Override
	public boolean hasChangedSince(final String modulePath, final List<String> childModules,
			final Collection<ProposedTag> tags) throws IOException {
		final RevWalk walk = new RevWalk(repo);
		try {
			walk.setRetainBody(false);
			walk.markStart(walk.parseCommit(repo.findRef("HEAD").getObjectId()));
			filterOutOtherModulesChanges(modulePath, childModules, walk);
			stopWalkingWhenTheTagsAreHit(tags, walk);
			return walk.iterator().hasNext();
		} finally {
			walk.dispose();
		}
	}

	private static void stopWalkingWhenTheTagsAreHit(final Collection<ProposedTag> tags, final RevWalk walk)
			throws IOException {
		for (final ProposedTag tag : tags) {
			final ObjectId commitId = tag.getObjectId();
			final RevCommit revCommit = walk.parseCommit(commitId);
			walk.markUninteresting(revCommit);
		}
	}

	private void filterOutOtherModulesChanges(final String modulePath, final List<String> childModules,
			final RevWalk walk) {
		final boolean isRootModule = ".".equals(modulePath);
		final boolean isMultiModuleProject = !isRootModule || !childModules.isEmpty();
		final List<TreeFilter> treeFilters = new LinkedList<TreeFilter>();
		treeFilters.add(TreeFilter.ANY_DIFF);
		if (isMultiModuleProject) {
			if (!isRootModule) {
				// for sub-modules, look for changes only in the sub-module
				// path...
				treeFilters.add(PathFilter.create(modulePath));
			}

			// ... but ignore any sub-modules of the current sub-module, because
			// they can change independently of the current module
			for (final String childModule : childModules) {
				final String path = isRootModule ? childModule : modulePath + "/" + childModule;
				treeFilters.add(PathFilter.create(path).negate());
			}

		}
		final TreeFilter treeFilter = treeFilters.size() == 1 ? treeFilters.get(0) : AndTreeFilter.create(treeFilters);
		walk.setTreeFilter(treeFilter);
	}
}
