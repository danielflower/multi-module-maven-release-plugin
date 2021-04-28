package com.github.danielflower.mavenplugins.release;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.logging.Log;

import java.util.List;

class ResolverWrapper {

    private final ArtifactFactory factory;
    private final ArtifactResolver artifactResolver;
    private final List remoteRepositories;
    private final ArtifactRepository localRepository;

    ResolverWrapper(BaseMojo.PluginConfiguration pluginConfiguration) {
        this.factory = pluginConfiguration.factory;
        this.artifactResolver = pluginConfiguration.artifactResolver;
        this.remoteRepositories = pluginConfiguration.remoteRepositories;
        this.localRepository = pluginConfiguration.localRepository;
    }

    boolean isResolvable(String groupId, String artifactId, String version, String type, Log log) {
        try {
            Artifact pomArtifact = this.factory.createArtifact(groupId, artifactId, version, "", type);
            artifactResolver.resolve(pomArtifact, remoteRepositories, localRepository);
            return true;

        } catch (ArtifactResolutionException e) {
            log.warn("can't resolve parent pom: " + e.getMessage());
        } catch (ArtifactNotFoundException e) {
            log.info("can't resolve artifact: " + e.getMessage());
        }

        return false;
    }
}
