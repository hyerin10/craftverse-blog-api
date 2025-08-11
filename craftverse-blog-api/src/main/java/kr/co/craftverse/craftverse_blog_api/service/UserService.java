package kr.co.craftverse.craftverse_blog_api.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.exception.DuplicateResourceException;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.NotFoundException;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.LoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.OAuthUserUpdateRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.UserRegistrationRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.UserResponseDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.UserUpdateRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.TokenRepository;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import kr.co.craftverse.craftverse_blog_api.service.messaging.EmailProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailProducer emailProducer;
  private final Logger logger;
  private final JwtTokenProvider jwtTokenProvider;
  private final EmailVerificationService emailVerificationService;
  private final TokenRepository tokenRepository;

  // 리프레시 토큰 만료 시간 (7일)
  private static final long REFRESH_TOKEN_EXPIRY_SECONDS = 7L * 24 * 60 * 60;

  /**
   * 현재 사용자 정보 조회
   */
  public UserResponseDTO getCurrentUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(UnauthorizedException::new);

    return UserResponseDTO.builder()
        .id(user.getId())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .email(user.getEmail())
        .birthDate(user.getBirthDate())
        .country(user.getCountry())
        .postalCode(user.getPostalCode())
        .emailVerified(user.isEmailVerified())
        .createdAt(user.getCreatedAt())
        .oauthProvider(user.getOauthProvider())
        .build();
  }

  @Transactional
  public UserResponseDTO patchUser(Long userId, UserUpdateRequestDTO updateRequest) {
    User user = userRepository.findById(userId)
        .orElseThrow(NotFoundException::new);

    // 이메일 중복 검사 (변경하려는 경우에만)
    if (updateRequest.getEmail() != null &&
        !updateRequest.getEmail().trim().isEmpty() &&
        !updateRequest.getEmail().equals(user.getEmail())) {
      if (userRepository.existsByEmail(updateRequest.getEmail()))
        throw new DuplicateResourceException();
    }

    // 엔티티의 patch 메서드를 사용하여 업데이트
    user.patchUserFromDto(updateRequest, passwordEncoder);

    User savedUser = userRepository.save(user);
    return UserResponseDTO.builder()
        .id(savedUser.getId())
        .firstName(savedUser.getFirstName())
        .lastName(savedUser.getLastName())
        .email(savedUser.getEmail())
        .birthDate(savedUser.getBirthDate() != null ? savedUser.getBirthDate() : null)
        .country(savedUser.getCountry())
        .postalCode(savedUser.getPostalCode())
        .createdAt(savedUser.getCreatedAt())
        .updatedAt(savedUser.getUpdatedAt())
        .emailVerified(savedUser.getEmailVerified())
        .build();
  }

  @Transactional
  public UserResponseDTO patchUser(Long userId, OAuthUserUpdateRequestDTO oAuthUserUpdateRequestDTO) {
    User user = userRepository.findById(userId)
        .orElseThrow(NotFoundException::new);

    // 엔티티의 patch 메서드를 사용하여 업데이트
    user.patchOAuthUserFromDto(oAuthUserUpdateRequestDTO, passwordEncoder);

    User savedUser = userRepository.save(user);
    return UserResponseDTO.builder()
        .id(savedUser.getId())
        .firstName(savedUser.getFirstName())
        .lastName(savedUser.getLastName())
        .birthDate(savedUser.getBirthDate() != null ? savedUser.getBirthDate() : null)
        .country(savedUser.getCountry())
        .postalCode(savedUser.getPostalCode())
        .createdAt(savedUser.getCreatedAt())
        .updatedAt(savedUser.getUpdatedAt())
        .emailVerified(savedUser.getEmailVerified())
        .build();
  }

  @Transactional
  public UserResponseDTO registerUser(UserRegistrationRequestDTO userRegistrationRequestDTO) {
    if (userRepository.existsByEmail(userRegistrationRequestDTO.getEmail()))
      throw new DuplicateResourceException();

    Long currentTime = Instant.now().getEpochSecond();

    User user = User.builder()
        .firstName(userRegistrationRequestDTO.getFirstName())
        .lastName(userRegistrationRequestDTO.getLastName())
        .email(userRegistrationRequestDTO.getEmail())
        .password(passwordEncoder.encode(userRegistrationRequestDTO.getPassword()))
        .birthDate(userRegistrationRequestDTO.getBirthDate())
        .country(userRegistrationRequestDTO.getCountry())
        .postalCode(userRegistrationRequestDTO.getPostalCode())
        .emailVerified(false)
        .createdAt(currentTime)
        .updatedAt(currentTime)
        .loginAttempts(0)
        .accountLocked(false)
        .build();

    User savedUser = userRepository.save(user);

    // 이메일 인증 서비스 사용
    emailVerificationService.sendEmailVerificationCode(savedUser.getEmail());

    logger.info("[UserService] 회원가입 완료 및 인증 이메일 발송 요청: {}", savedUser.getEmail());

    return UserResponseDTO.builder()
        .id(savedUser.getId())
        .firstName(savedUser.getFirstName())
        .lastName(savedUser.getLastName())
        .email(savedUser.getEmail())
        .birthDate(savedUser.getBirthDate())
        .country(savedUser.getCountry())
        .postalCode(savedUser.getPostalCode())
        .createdAt(savedUser.getCreatedAt())
        .build();
  }

  @Transactional
  public void resendVerificationEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(NotFoundException::new);

    if (user.isEmailVerified()) {
      logger.info("[UserService] 이미 인증된 이메일입니다: {}", email);
      return;
    }

    // 이메일 인증 서비스 사용
    emailVerificationService.sendEmailVerificationCode(email);
    logger.info("[UserService] 인증 이메일 재발송: {}", email);
  }

  public User findById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(NotFoundException::new);
  }

  @Transactional
  public Map<String, String> login(LoginRequestDTO loginRequestDTO) {
    // 이메일로 사용자 조회
    User user = userRepository.findByEmail(loginRequestDTO.getEmail())
        .orElseThrow(UnauthorizedException::new);

    // 비밀번호 확인
    if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword()))
      throw new UnauthorizedException();

    // JWT 액세스 토큰 생성
    String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());

    // 리프레시 토큰 생성 및 저장
    String refreshToken = generateRefreshToken();
    tokenRepository.saveRefreshToken(user.getId(), refreshToken, REFRESH_TOKEN_EXPIRY_SECONDS);

    userRepository.save(user);

    Map<String, String> tokens = new HashMap<>();
    tokens.put("accessToken", accessToken);
    tokens.put("refreshToken", refreshToken);

    return tokens;
  }

  /**
   * 일반 사용자 리프레시 토큰 갱신 (RTR 적용)
   */
  @Transactional
  public Map<String, String> refreshAccessToken(Long userId, String email, String refreshToken) {
    // Redis에서 리프레시 토큰 확인
    String storedRefreshToken = tokenRepository.getRefreshToken(userId);

    if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
      throw new UnauthorizedException();
    }

    // 사용자 존재 여부 확인
    User user = userRepository.findById(userId)
        .orElseThrow(UnauthorizedException::new);

    // 이메일 일치 확인
    if (!user.getEmail().equals(email)) {
      throw new UnauthorizedException();
    }

    // 기존 리프레시 토큰 삭제 (RTR)
    tokenRepository.deleteRefreshToken(userId);

    // 새로운 토큰들 생성
    String newAccessToken = jwtTokenProvider.createAccessToken(userId, email);
    String newRefreshToken = generateRefreshToken();

    // 새로운 리프레시 토큰 저장
    tokenRepository.saveRefreshToken(userId, newRefreshToken, REFRESH_TOKEN_EXPIRY_SECONDS);

    Map<String, String> tokens = new HashMap<>();
    tokens.put("accessToken", newAccessToken);
    tokens.put("refreshToken", newRefreshToken);

    logger.info("[UserService] 일반 사용자 토큰 갱신 완료: userId={}, email={}", userId, email);

    return tokens;
  }

  /**
   * 리프레시 토큰 생성
   */
  private String generateRefreshToken() {
    return UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis();
  }

  /**
   * 로그아웃 처리
   * JWT 토큰을 블랙리스트에 추가하여 무효화
   */
  @Transactional
  public void logout(HttpServletRequest request) {
    String token = jwtTokenProvider.resolveToken(request);

    if (token != null && jwtTokenProvider.validateToken(token)) {
      // 토큰을 블랙리스트에 추가
      jwtTokenProvider.blacklistToken(token);

      // 모든 토큰 삭제
      Long userId = jwtTokenProvider.getUserId(token);
      tokenRepository.deleteTokens(userId);

      String email = jwtTokenProvider.getEmail(token);
      logger.info("[UserService] 로그아웃 완료: userId={}, email={}", userId, email);
    } else {
      logger.warn("[UserService] 유효하지 않은 토큰으로 로그아웃 시도");
    }
  }

  /**
   * 계정 삭제 처리
   * 사용자 정보 삭제 및 JWT 토큰 무효화
   */
  @Transactional
  public void delete(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(UnauthorizedException::new);

    // 모든 토큰 삭제
    tokenRepository.deleteTokens(userId);

    userRepository.delete(user);
    logger.info("[UserService] 계정 삭제 완료: userId={}, email={}", userId, user.getEmail());
  }
}