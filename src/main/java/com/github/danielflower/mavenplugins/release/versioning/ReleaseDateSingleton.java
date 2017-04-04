package com.github.danielflower.mavenplugins.release.versioning;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class ReleaseDateSingleton {
    private static final ReleaseDateSingleton INSTANCE;
    private static final DateTimeFormatter FILE_SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");
    private static final ZoneId UTC = ZoneId.of("UTC");

    static {
        INSTANCE = new ReleaseDateSingleton();
    }

    private final ZonedDateTime RELEASE_DATE = ZonedDateTime.now();

    private ReleaseDateSingleton() {
    }

    public static ReleaseDateSingleton getInstance() {
        return INSTANCE;
    }

    public ZonedDateTime releaseDate() {
        return RELEASE_DATE;
    }

    public String tagName() {
        return "MULTI_MODULE_RELEASE-" + RELEASE_DATE.withZoneSameInstant(UTC).format(FILE_SUFFIX_FORMATTER);
    }
}
