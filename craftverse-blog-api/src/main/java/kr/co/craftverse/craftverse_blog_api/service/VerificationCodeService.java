package kr.co.craftverse.craftverse_blog_api.service;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.VERIFICATION_CODE_FORMAT;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.VERIFICATION_CODE_MAX_VALUE;

import java.time.Duration;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerificationCodeService {

  private final RedisTemplate<String, String> redisTemplate;
  private final Logger logger;

  @Value("${verification.code.expiry:900}")
  private Long verificationCodeExpirySeconds;

  /**
   * 6자리 랜덤 인증 코드 생성
   */
  public String generateVerificationCode() {
    Random random = new Random();
    return String.format(VERIFICATION_CODE_FORMAT, random.nextInt(VERIFICATION_CODE_MAX_VALUE));
  }

  /**
   * 인증 코드 저장 (Redis)
   */
  public void saveVerificationCode(String key, String code) {
    redisTemplate.opsForValue().set(key, code, Duration.ofSeconds(verificationCodeExpirySeconds));
    logger.info("[VerificationCodeService] 인증 코드 저장 완료: key={}", key);
  }

  /**
   * 인증 코드 검증
   */
  public boolean verifyCode(String key, String inputCode) {
    String storedCode = redisTemplate.opsForValue().get(key);

    if (storedCode == null || storedCode.isEmpty()) {
      logger.warn("[VerificationCodeService] 인증 코드가 존재하지 않거나 만료됨: key={}", key);
      return false;
    }

    boolean isValid = storedCode.equals(inputCode);

    if (isValid) {
      // 인증 성공 시 코드 삭제
      redisTemplate.delete(key);
      logger.info("[VerificationCodeService] 인증 코드 검증 성공: key={}", key);
    } else {
      logger.warn("[VerificationCodeService] 인증 코드 불일치: key={}", key);
    }

    return isValid;
  }

  /**
   * 인증 코드 삭제 (재사용 방지 및 보안)
   */
  public void deleteCode(String key) {
    Boolean deleted = redisTemplate.delete(key);

    if (Boolean.TRUE.equals(deleted)) {
      logger.info("[VerificationCodeService] 인증 코드 삭제 완료: key={}", key);
    } else {
      logger.warn("[VerificationCodeService] 삭제할 인증 코드가 존재하지 않음: key={}", key);
    }
  }

  /**
   * 인증 코드 존재 여부 확인
   */
  public boolean existsCode(String key) {
    Boolean exists = redisTemplate.hasKey(key);
    logger.debug("[VerificationCodeService] 인증 코드 존재 여부 확인: key={}, exists={}", key, exists);
    return Boolean.TRUE.equals(exists);
  }

  /**
   * 인증 코드 남은 만료 시간 조회 (초 단위)
   */
  public Long getCodeTtl(String key) {
    Long ttl = redisTemplate.getExpire(key);
    logger.debug("[VerificationCodeService] 인증 코드 남은 시간: key={}, ttl={}초", key, ttl);
    return ttl;
  }
}