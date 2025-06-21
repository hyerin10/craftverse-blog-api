package kr.co.craftverse.craftverse_blog_api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "toss.payments")
public class TossPaymentProperties {

  private String clientKey;
  private String secretKey;
  private String apiUrl;
  private String successUrl;
  private String failUrl;
  private String webhookUrl;
}