package kr.co.craftverse.craftverse_blog_api.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Value("${spring.redis.host}")
  private String redisHost;

  @Value("${spring.redis.port}")
  private int redisPort;

  @Value("${spring.redis.password}")
  private String redisPassword;

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
    redisConfig.setPassword(redisPassword);
    return new LettuceConnectionFactory(redisConfig);
  }
  // 1. 기존에 쓰던 템플릿 (Object 타입)
  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory());
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
    return redisTemplate;
  }

  // 2. 선착순 로직을 위해 반환 타입을 List로 지정한 스크립트 빈
  // 빈 이름을 "promotionScript"로 명시하여 구분하기 편하게 합니다.
  @Bean(name = "promotionScript")
  public DefaultRedisScript<List> promotionScript() {
    DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
    redisScript.setLocation(new ClassPathResource("scripts/promotion.lua"));
    redisScript.setResultType(List.class); // Lua에서 {1, remain}을 반환하므로 List 필수
    return redisScript;
  }
}