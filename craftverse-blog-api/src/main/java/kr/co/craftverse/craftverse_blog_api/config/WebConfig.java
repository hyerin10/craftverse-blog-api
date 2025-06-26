package kr.co.craftverse.craftverse_blog_api.config;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.CORS_ALLOWED_HEADERS;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.CORS_ALLOWED_METHODS;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.CORS_ALLOWED_ORIGIN;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.CORS_MAPPING_PATTERN;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping(CORS_MAPPING_PATTERN)
        .allowedOrigins(CORS_ALLOWED_ORIGIN)
        .allowedMethods(CORS_ALLOWED_METHODS)
        .allowedHeaders(CORS_ALLOWED_HEADERS)
        .allowCredentials(true);
  }
}