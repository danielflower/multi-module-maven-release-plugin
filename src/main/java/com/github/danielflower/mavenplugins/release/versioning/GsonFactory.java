package com.github.danielflower.mavenplugins.release.versioning;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

public class GsonFactory {
    public Gson createGson() {
        final GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        builder.registerTypeAdapter(ZonedDateTime.class,
                                    (JsonDeserializer<ZonedDateTime>) (json, type, jsonDeserializationContext) -> ZonedDateTime
                                                                                                          .parse(json.getAsJsonPrimitive().getAsString()));

        builder.registerTypeAdapter(ZonedDateTime.class,
                                    (JsonSerializer<ZonedDateTime>) (dateTime, type, jsonSerializationContext) ->
                                                                        jsonSerializationContext.serialize(dateTime.format
                                                                                                              (DateTimeFormatter
                                                                                            .ISO_DATE_TIME)));

        return builder.create();
    }
}
