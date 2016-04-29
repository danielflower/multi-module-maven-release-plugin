package com.github.danielflower.mavenplugins.release.scm;

import java.io.IOException;
import java.util.Collection;

public interface DiffDetector {
    boolean hasChangedSince(String modulePath, java.util.List<String> childModules, Collection<AnnotatedTag> tags) throws IOException;
}
