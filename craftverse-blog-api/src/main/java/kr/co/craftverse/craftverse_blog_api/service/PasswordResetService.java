package kr.co.craftverse.craftverse_blog_api.service;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PASSWORD_RESET_PREFIX;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PASSWORD_RESET_VERIFIED_PREFIX;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PASSWORD_RESET_VERIFIED_VALUE;

import kr.co.craftverse.craftverse_blog_api.common.exception.http.NotFoundException;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.model.dto.ResetPasswordRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.VerifyPasswordResetDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import kr.co.craftverse.craftverse_blog_api.service.messaging.EmailProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private final VerificationCodeService verificationCodeService;
  private final UserRepository userRepository;
  private final EmailProducer emailProducer;
  private final PasswordEncoder passwordEncoder;
  private final Logger logger;

  /**
   * 비밀번호 재설정 인증 코드 발송
   */
  public void sendPasswordResetCode(String email) {
    // 사용자 존재 확인
    User user = userRepository.findByEmail(email)
        .orElseThrow(NotFoundException::new);

    String code = verificationCodeService.generateVerificationCode();
    String key = PASSWORD_RESET_PREFIX + email;

    // Redis에 인증 코드 저장
    verificationCodeService.saveVerificationCode(key, code);

    // 비밀번호 재설정 이메일 발송 요청 (새로운 메서드 필요)
    emailProducer.sendPasswordResetEmail(email, code);

    logger.info("[PasswordResetService] 비밀번호 재설정 코드 발송: {}", email);
  }

  /**
   * 비밀번호 재설정 완료 - 중복 검증 제거
   */
  public void resetPassword(ResetPasswordRequestDTO resetPasswordRequestDTO) {
    String email = resetPasswordRequestDTO.getEmail();
    String code = resetPasswordRequestDTO.getCode();
    String newPassword = resetPasswordRequestDTO.getNewPassword();

    logger.info("[PasswordResetService] 비밀번호 재설정 요청: {}", email);

    // 1. 인증 코드 검증 (이미 /verify-code 단계에서 검증되었지만 보안을 위해 한 번 더 확인)
    String codeKey = PASSWORD_RESET_PREFIX + email;

    // 먼저 코드가 존재하는지 확인
    if (!verificationCodeService.existsCode(codeKey)) {
      // 코드가 없다면 이미 사용되었거나 만료된 것
      // 하지만 비밀번호 재설정 프로세스에서는 코드가 이미 검증되어 삭제된 상태일 수 있음
      // 따라서 검증된 상태임을 표시하는 별도 키를 확인
      String verifiedKey = PASSWORD_RESET_VERIFIED_PREFIX + email;

      if (!verificationCodeService.existsCode(verifiedKey)) {
        logger.warn("[PasswordResetService] 인증되지 않은 비밀번호 재설정 요청: {}", email);
        throw new UnauthorizedException();
      }

      logger.info("[PasswordResetService] 이미 검증된 요청으로 진행: {}", email);
    } else {
      // 코드가 존재한다면 검증 후 삭제
      boolean isCodeValid = verificationCodeService.verifyCode(codeKey, code);
      if (!isCodeValid) {
        logger.warn("[PasswordResetService] 유효하지 않은 인증 코드: {}", email);
        throw new UnauthorizedException();
      }
    }

    // 2. 사용자 조회
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          logger.warn("[PasswordResetService] 사용자를 찾을 수 없음: {}", email);
          return new UnauthorizedException();
        });

    // 3. 비밀번호 암호화 및 업데이트
    String encodedPassword = passwordEncoder.encode(newPassword);
    user.updatePassword(encodedPassword);
    userRepository.save(user);

    // 4. 검증 완료 표시 키도 삭제 (있다면)
    String verifiedKey = PASSWORD_RESET_VERIFIED_PREFIX + email;
    verificationCodeService.deleteCode(verifiedKey);

    logger.info("[PasswordResetService] 비밀번호 재설정 완료: {}", email);
  }

  /**
   * 비밀번호 재설정 코드 검증 - 검증 완료 표시 추가
   */
  public boolean verifyPasswordResetCode(VerifyPasswordResetDTO verifyPasswordResetDTO) {
    String email = verifyPasswordResetDTO.getEmail();
    String inputCode = verifyPasswordResetDTO.getCode();

    logger.info("[PasswordResetService] 비밀번호 재설정 코드 검증 요청: {}", email);

    // 사용자 존재 여부 확인
    User user = userRepository.findByEmail(email)
        .orElseThrow(UnauthorizedException::new);

    String codeKey = PASSWORD_RESET_PREFIX + email;
    boolean isValid = verificationCodeService.verifyCode(codeKey, inputCode);

    if (isValid) {
      // 검증 성공 시 임시 검증 완료 표시 저장 (5분 동안 유효)
      String verifiedKey = PASSWORD_RESET_VERIFIED_PREFIX + email;
      verificationCodeService.saveVerificationCode(verifiedKey, PASSWORD_RESET_VERIFIED_VALUE); // 5분

      logger.info("[PasswordResetService] 비밀번호 재설정 코드 검증 성공: {}", email);
    } else {
      logger.warn("[PasswordResetService] 비밀번호 재설정 코드 검증 실패: {}", email);
    }

    return isValid;
  }
}