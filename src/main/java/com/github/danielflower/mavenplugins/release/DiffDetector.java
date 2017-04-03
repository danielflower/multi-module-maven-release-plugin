package com.github.danielflower.mavenplugins.release;

import java.io.IOException;

import org.eclipse.jgit.lib.Ref;

public interface DiffDetector {

    boolean hasChangedSince(String modulePath, java.util.List<String> childModules, Ref tagReference) throws IOException;
}
