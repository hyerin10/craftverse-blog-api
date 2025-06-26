package kr.co.craftverse.craftverse_blog_api.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TossWebhookSecurityValidator {

  // 토스페이먼츠 웹훅 서버 IP 대역 (실제 운영시 토스페이먼츠 공식 IP로 업데이트 필요)
  private static final List<String> TOSS_WEBHOOK_IPS = Arrays.asList(
      "52.78.100.19",
      "52.78.48.223",
      "52.78.5.241"
      // 토스페이먼츠 공식 문서에서 최신 IP 확인 필요
  );

  private final Logger logger;

  /**
   * 웹훅 요청 보안 검증
   */
  public boolean validateWebhookRequest(HttpServletRequest request) {
    try {
      // 1. IP 검증 (운영환경에서만)
      if (isProductionEnvironment()) {
        if (!isValidTossIP(request)) {
          logger.warn("허용되지 않은 IP에서 웹훅 요청: {}", getClientIpAddress(request));
          return false;
        }
      }

      // 2. HTTPS 검증 (운영환경에서만)
      if (isProductionEnvironment()) {
        if (!isHttpsRequest(request)) {
          logger.warn("HTTPS가 아닌 웹훅 요청");
          return false;
        }
      }

      // 3. User-Agent 검증 (선택사항)
      String userAgent = request.getHeader("User-Agent");
      if (userAgent != null && !userAgent.contains("TossPayments")) {
        logger.warn("의심스러운 User-Agent: {}", userAgent);
        // 개발환경에서는 경고만, 운영환경에서는 차단 고려
      }

      return true;

    } catch (Exception e) {
      logger.error("웹훅 보안 검증 중 오류 발생", e);
      return false;
    }
  }

  /**
   * 토스페이먼츠 IP인지 확인
   */
  private boolean isValidTossIP(HttpServletRequest request) {
    String clientIP = getClientIpAddress(request);
    return TOSS_WEBHOOK_IPS.contains(clientIP);
  }

  /**
   * HTTPS 요청인지 확인
   */
  private boolean isHttpsRequest(HttpServletRequest request) {
    return request.isSecure() ||
        "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
  }

  /**
   * 운영환경인지 확인
   */
  private boolean isProductionEnvironment() {
    // 환경변수나 프로파일로 판단
    String profile = System.getProperty("spring.profiles.active");
    return "prod".equals(profile) || "production".equals(profile);
  }

  /**
   * 클라이언트 실제 IP 주소 추출
   */
  private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }
}