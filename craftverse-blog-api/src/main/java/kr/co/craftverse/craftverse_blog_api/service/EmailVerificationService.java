package kr.co.craftverse.craftverse_blog_api.service;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.EMAIL_VERIFICATION_PREFIX;

import jakarta.transaction.Transactional;
import java.time.Duration;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.model.dto.VerifyEmailDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final RedisTemplate<String, String> redisTemplate;
  private final UserRepository userRepository;

  private final Logger logger;

  @Value("${verification.code.expiry:900}")
  private Long verificationCodeExpirySeconds;

  /**
   * 이메일 인증 코드를 Redis에 저장
   */
  public void saveVerificationCode(String email, String code) {
    String key = EMAIL_VERIFICATION_PREFIX + email;
    redisTemplate.opsForValue().set(key, code, Duration.ofSeconds(verificationCodeExpirySeconds));
    logger.info("[VerificationService] 인증 코드 저장 완료 (Redis): {}", email);
  }

  /**
   * 이메일 인증 코드 검증
   */
  @Transactional
  public boolean verifyEmail(VerifyEmailDTO verifyEmailDTO) {
    String key = EMAIL_VERIFICATION_PREFIX + verifyEmailDTO.getEmail();
    String storedCode = redisTemplate.opsForValue().get(key);

    if (storedCode.isEmpty() || !storedCode.equals(verifyEmailDTO.getCode()))
      throw new UnauthorizedException();

    User user = userRepository.findByEmail(verifyEmailDTO.getEmail())
        .orElseThrow(UnauthorizedException::new);

    // 사용자가 이미 인증되었는지 확인
    if (user.isEmailVerified()) {
      logger.info("[VerificationService] 이미 인증된 이메일입니다: {}", verifyEmailDTO.getEmail());
      // 사용된 인증 코드 삭제
      redisTemplate.delete(key);
      // return ture가 아닌 보안상 예외처리
      throw new UnauthorizedException();
    }

    // 이메일 인증 상태 업데이트
    user.verifyEmail();
    userRepository.save(user);

    // 사용된 인증 코드 삭제
    redisTemplate.delete(key);

    logger.info("[VerificationService] 이메일 인증 완료: {}", verifyEmailDTO.getEmail());
    return true;
  }
}