package kr.co.craftverse.craftverse_blog_api.service;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.EMAIL_VERIFICATION_PREFIX;

import jakarta.transaction.Transactional;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.model.dto.VerifyEmailDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import kr.co.craftverse.craftverse_blog_api.service.messaging.EmailProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final VerificationCodeService verificationCodeService;
  private final UserRepository userRepository;
  private final EmailProducer emailProducer;
  private final Logger logger;

  /**
   * 이메일 인증 코드 생성 및 발송
   */
  public void sendEmailVerificationCode(String email) {
    String code = verificationCodeService.generateVerificationCode();
    String key = EMAIL_VERIFICATION_PREFIX + email;

    // Redis에 인증 코드 저장
    verificationCodeService.saveVerificationCode(key, code);

    // 이메일 발송 요청 (코드와 함께)
    emailProducer.sendVerificationEmail(email, code);

    logger.info("[EmailVerificationService] 이메일 인증 코드 발송: {}", email);
  }

  /**
   * 이메일 인증 코드 검증 및 사용자 이메일 인증 상태 업데이트
   */
  @Transactional
  public boolean verifyEmail(VerifyEmailDTO verifyEmailDTO) {
    String key = EMAIL_VERIFICATION_PREFIX + verifyEmailDTO.getEmail();

    // 인증 코드 검증
    if (!verificationCodeService.verifyCode(key, verifyEmailDTO.getCode())) {
      throw new UnauthorizedException();
    }

    // 사용자 조회
    User user = userRepository.findByEmail(verifyEmailDTO.getEmail())
        .orElseThrow(UnauthorizedException::new);

    // 이미 인증된 경우 예외 처리
    if (user.isEmailVerified()) {
      logger.info("[EmailVerificationService] 이미 인증된 이메일입니다: {}", verifyEmailDTO.getEmail());
      throw new UnauthorizedException();
    }

    // 이메일 인증 상태 업데이트
    user.verifyEmail();
    userRepository.save(user);

    logger.info("[EmailVerificationService] 이메일 인증 완료: {}", verifyEmailDTO.getEmail());
    return true;
  }
}