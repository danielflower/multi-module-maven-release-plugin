package de.hilling.maven.release.versioning;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class ReleaseDateSingleton {
    public static final String            TAG_PREFIX            = "MULTI_MODULE_RELEASE-";
    public static final DateTimeFormatter FILE_SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");
    public static final ZoneId            RELEASE_DATE_TIMEZONE = ZoneId.of("UTC");

    private static final ReleaseDateSingleton INSTANCE;

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
        return TAG_PREFIX + RELEASE_DATE.withZoneSameInstant(RELEASE_DATE_TIMEZONE).format(FILE_SUFFIX_FORMATTER);
    }
}
