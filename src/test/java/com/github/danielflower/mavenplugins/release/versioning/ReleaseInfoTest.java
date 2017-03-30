package com.github.danielflower.mavenplugins.release.versioning;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

public class ReleaseInfoTest {

    private static final GsonFactory GSON_FACTORY = new GsonFactory();
    private static final String TEST_RELEASE_INFO;
    private static final ZonedDateTime REFERENCE_DATE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1030042000000L),
                                                                                ZoneId.of("Europe/Berlin"));

    static {
        try {
            InputStream resource = ReleaseInfoTest.class.getResourceAsStream("/test-releaseinfo.json");
            TEST_RELEASE_INFO = IOUtils.toString(resource, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new RuntimeException("unable to load resource");
        }
    }

    private Gson                           gson;
    private ImmutableReleaseInfo.Builder   infoBuilder;
    private ImmutableFixVersion.Builder    versionBuilder;
    private ImmutableModuleVersion.Builder modulerBuilder;

    @Before
    public void setUp() {
        gson = GSON_FACTORY.createGson();
        infoBuilder = ImmutableReleaseInfo.builder();
        infoBuilder.tagName("sampletag");
        modulerBuilder = ImmutableModuleVersion.builder();
        modulerBuilder.name("module-1");
        modulerBuilder.releaseDate(REFERENCE_DATE);
        versionBuilder = ImmutableFixVersion.builder();
        versionBuilder.majorVersion(3);
        versionBuilder.minorVersion(2);
        modulerBuilder.version(versionBuilder.build());
    }

    @Test
    public void serialize() {
        final String json = gson.toJson(infoBuilder.addModules(modulerBuilder.build()).build());
        assertEquals(TEST_RELEASE_INFO, json);
    }

    @Test
    public void deserialize() {
        final ReleaseInfo releaseInfo = gson.fromJson(TEST_RELEASE_INFO, ImmutableReleaseInfo.class);
        assertEquals(infoBuilder.addModules(modulerBuilder.build()).build(), releaseInfo);
    }
}