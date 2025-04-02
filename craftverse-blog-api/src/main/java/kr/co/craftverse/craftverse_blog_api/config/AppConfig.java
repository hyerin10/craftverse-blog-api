package kr.co.craftverse.craftverse_blog_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

  @Bean
  public Logger applicationLogger() {
    return LoggerFactory.getLogger("kr.co.craftverse.craftverse_blog_api");
  }
}
