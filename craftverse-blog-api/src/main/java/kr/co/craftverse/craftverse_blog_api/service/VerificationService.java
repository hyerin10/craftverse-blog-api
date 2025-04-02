package kr.co.craftverse.craftverse_blog_api.service;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.EMAIL_VERIFICATION_PREFIX;

import jakarta.transaction.Transactional;
import java.time.Duration;
import kr.co.craftverse.craftverse_blog_api.exception.ResourceNotFoundException;
import kr.co.craftverse.craftverse_blog_api.model.dto.VerifyEmailDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerificationService {

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
  public boolean verifyEmail(VerifyEmailDTO verifyEmailDTO) throws BadRequestException {
    String key = EMAIL_VERIFICATION_PREFIX + verifyEmailDTO.getEmail();
    String storedCode = redisTemplate.opsForValue().get(key);

    if (storedCode == null)
      throw new BadRequestException("인증 코드가 만료되었거나 존재하지 않습니다.");

    if (!storedCode.equals(verifyEmailDTO.getCode()))
      throw new BadRequestException("인증 코드가 일치하지 않습니다.");

    User user = userRepository.findByEmail(verifyEmailDTO.getEmail())
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + verifyEmailDTO.getEmail()));

    // 사용자가 이미 인증되었는지 확인
    if (user.isEmailVerified()) {
      logger.info("[VerificationService] 이미 인증된 이메일입니다: {}", verifyEmailDTO.getEmail());
      // 사용된 인증 코드 삭제
      redisTemplate.delete(key);
      return true;
    }

    // 이메일 인증 상태 업데이트
    user.verifyEmail();
    userRepository.save(user);

    // 사용된 인증 코드 삭제
    redisTemplate.delete(key);

    logger.info("[VerificationService] 이메일 인증 완료: {}", verifyEmailDTO.getEmail());
    return true;
  }

  /**
   * 인증 코드가 유효한지 확인
   */
  public boolean isVerificationCodeValid(String email, String code) {
    String key = EMAIL_VERIFICATION_PREFIX + email;
    String storedCode = redisTemplate.opsForValue().get(key);
    return storedCode != null && storedCode.equals(code);
  }
}