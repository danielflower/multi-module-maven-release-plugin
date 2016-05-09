package com.github.danielflower.mavenplugins.release.scm;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface DiffDetector {

	boolean hasChangedSince(String modulePath, List<String> childModules, Collection<ProposedTag> tags)
			throws IOException;
}
