package com.github.danielflower.mavenplugins.testprojects.versioninheritor;

import com.github.danielflower.mavenplugins.tesetprojects.openapispecasplugindependency.facade.openapi.api.PingApi;
import com.github.danielflower.mavenplugins.tesetprojects.openapispecasplugindependency.facade.openapi.model.PingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.OffsetDateTime;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api")
public class OpenApiController implements PingApi {

    @Override
    public ResponseEntity<PingResult> ping() {
        var result = new PingResult(OffsetDateTime.now());
        return ResponseEntity.ok(result);
    }

}
