package cn.studypilot.common;

import cn.studypilot.common.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoint used to verify exception serialization. */
@RestController
public class MissingResourceEndpoint {
  @GetMapping("/test/missing")
  String missing() {
    throw new ResourceNotFoundException("resource does not exist");
  }
}
