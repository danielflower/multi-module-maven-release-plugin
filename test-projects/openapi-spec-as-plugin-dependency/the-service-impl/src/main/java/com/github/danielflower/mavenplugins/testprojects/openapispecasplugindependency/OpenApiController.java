package com.github.danielflower.mavenplugins.testprojects.versioninheritor;

import com.github.danielflower.mavenplugins.tesetprojects.openapispecasplugindependency.facade.openapi.api.PingApi;
import com.github.danielflower.mavenplugins.tesetprojects.openapispecasplugindependency.facade.openapi.model.PingResult;
import java.time.OffsetDateTime;

public class OpenApiController {


    public static void main(String[] args) {
        // Not really a controller, but at least it uses the classes PingResult
        PingResult x = new PingResult();
    }


}
