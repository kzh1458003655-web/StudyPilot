package cn.studypilot.common;

import cn.studypilot.common.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Test-only endpoint used to verify exception serialization. */
@RestController
public class MissingResourceEndpoint {
  @GetMapping("/test/missing")
  String missing() {
    throw new ResourceNotFoundException("resource does not exist");
  }

  @GetMapping("/test/oversized-upload")
  String oversizedUpload() {
    throw new MaxUploadSizeExceededException(25L * 1024 * 1024);
  }
}
