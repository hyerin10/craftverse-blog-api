package kr.co.craftverse.craftverse_blog_api.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

  @GetMapping("/health")
  public RestResult<Map<String, String>> healthCheck() {
    Map<String, String> data = new LinkedHashMap<>();
    data.put("status", "UP");
    data.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    data.put("service", "craftverse-blog-api");
    return new RestResult<>(data);
  }
}
