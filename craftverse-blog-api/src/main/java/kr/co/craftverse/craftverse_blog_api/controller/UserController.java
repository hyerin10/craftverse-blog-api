package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.model.dto.LoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserRegistrationRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserResponseDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.VerifyEmailDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.service.AuthService;
import kr.co.craftverse.craftverse_blog_api.service.OAuth2Service;
import kr.co.craftverse.craftverse_blog_api.service.UserService;
import kr.co.craftverse.craftverse_blog_api.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
@Validated
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final AuthService authService;
  private final VerificationService verificationService;
  private final OAuth2Service oAuth2Service;
  private final JwtTokenProvider jwtTokenProvider;

  // === 기존 메서드들 ===

  @PostMapping("/register")
  public RestResult<Map<String, Object>> registerUser(@Valid @RequestBody UserRegistrationRequestDTO userRegistrationRequestDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    UserResponseDTO userResponseDTO = userService.registerUser(userRegistrationRequestDTO);
    data.put("user", userResponseDTO);
    data.put("message", "회원가입이 진행되었습니다. 이메일을 확인하여 인증을 완료해주세요.");
    return new RestResult<>(data);
  }

  @PostMapping("/login")
  public RestResult<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    String accessToken = authService.login(loginRequestDTO);
    data.put("accessToken", accessToken);
    return new RestResult<>(data);
  }

  @PostMapping("/verify-email")
  public RestResult<Map<String, Object>> verifyEmail(@Valid @RequestBody VerifyEmailDTO verifyEmailDTO)
      throws BadRequestException {
    Map<String, Object> data = new LinkedHashMap<>();
    boolean verified = verificationService.verifyEmail(verifyEmailDTO);
    data.put("verified", verified);
    data.put("message", "이메일 인증이 완료되었습니다.");
    return new RestResult<>(data);
  }

  @PostMapping("/resend-verification")
  public RestResult<Map<String, Object>> resendVerification(@RequestParam String email) {
    Map<String, Object> data = new LinkedHashMap<>();
    userService.resendVerificationEmail(email);
    data.put("message", "인증 이메일이 재발송되었습니다.");
    return new RestResult<>(data);
  }

  @PostMapping("/logout")
  public RestResult<Map<String, Object>> logout(HttpServletRequest request) {
    Map<String, Object> data = new LinkedHashMap<>();
    String token = extractTokenFromRequest(request);

    if (token != null) {
      jwtTokenProvider.blacklistToken(token);
      data.put("message", "로그아웃 되었습니다.");
    } else {
      data.put("message", "인증 토큰이 없습니다.");
    }

    return new RestResult<>(data);
  }

  // === 새로 추가된 메서드들 ===

  /**
   * 토큰 유효성 검증 API
   */
  @GetMapping("/validate-token")
  public RestResult<Map<String, Object>> validateToken(HttpServletRequest request) {
    Map<String, Object> data = new LinkedHashMap<>();

    try {
      String token = extractTokenFromRequest(request);

      if (token != null && jwtTokenProvider.validateToken(token)) {
        // JwtTokenProvider의 실제 메서드명 사용
        Long userId = jwtTokenProvider.getUserId(token);
        String email = jwtTokenProvider.getEmail(token);

        data.put("valid", true);
        data.put("userId", userId);
        data.put("email", email);
      } else {
        data.put("valid", false);
        data.put("message", "유효하지 않은 토큰입니다.");
      }
    } catch (Exception e) {
      log.error("토큰 검증 중 오류", e);
      data.put("valid", false);
      data.put("message", "토큰 검증 중 오류가 발생했습니다: " + e.getMessage());
    }

    return new RestResult<>(data);
  }

  /**
   * 현재 로그인한 사용자 정보 조회
   */
  @GetMapping("/me")
  public RestResult<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
    Map<String, Object> data = new LinkedHashMap<>();

    try {
      String token = extractTokenFromRequest(request);

      if (token == null) {
        data.put("error", "인증 토큰이 없습니다.");
        return new RestResult<>(data);
      }

      if (!jwtTokenProvider.validateToken(token)) {
        data.put("error", "유효하지 않은 토큰입니다.");
        return new RestResult<>(data);
      }

      Long userId = jwtTokenProvider.getUserId(token);
      User user = userService.findById(userId);

      Map<String, Object> userData = createUserResponse(user);
      return new RestResult<>(userData);

    } catch (Exception e) {
      log.error("현재 사용자 정보 조회 중 오류", e);
      data.put("error", "사용자 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
      return new RestResult<>(data);
    }
  }

  /**
   * 특정 사용자 정보 조회 (ID로)
   */
  @GetMapping("/{userId}")
  public RestResult<Map<String, Object>> getUserById(@PathVariable Long userId) {
    Map<String, Object> data = new LinkedHashMap<>();

    try {
      User user = userService.findById(userId);
      Map<String, Object> userData = createUserResponse(user);
      return new RestResult<>(userData);

    } catch (Exception e) {
      log.error("사용자 정보 조회 중 오류", e);
      data.put("error", "사용자 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
      return new RestResult<>(data);
    }
  }

  // === 유틸리티 메서드들 ===

  /**
   * 요청에서 JWT 토큰 추출 (쿠키 우선, Authorization 헤더 보조)
   */
  private String extractTokenFromRequest(HttpServletRequest request) {
    // 1. 쿠키에서 토큰 찾기 (Google OAuth 로그인의 경우)
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("auth_token".equals(cookie.getName())) {
          log.debug("쿠키에서 토큰 발견: {}", cookie.getName());
          return cookie.getValue();
        }
      }
    }

    // 2. Authorization 헤더에서 토큰 찾기 (일반 로그인의 경우)
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      log.debug("Authorization 헤더에서 토큰 발견");
      return bearerToken.substring(7);
    }

    // 3. JwtTokenProvider의 기본 메서드 사용 (fallback)
    String fallbackToken = jwtTokenProvider.resolveToken(request);
    if (fallbackToken != null) {
      log.debug("fallback으로 토큰 발견");
    }

    return fallbackToken;
  }

  /**
   * 사용자 응답 DTO 생성
   */
  private Map<String, Object> createUserResponse(User user) {
    Map<String, Object> userData = new LinkedHashMap<>();
    userData.put("id", user.getId());
    userData.put("email", user.getEmail());
    userData.put("firstName", user.getFirstName());
    userData.put("lastName", user.getLastName());
    userData.put("profilePictureUrl", user.getProfilePictureUrl());
    userData.put("emailVerified", user.getEmailVerified());
    userData.put("oauthProvider", user.getOauthProvider());
    userData.put("lastLogin", user.getLastLogin());
    userData.put("createdAt", user.getCreatedAt());
    userData.put("updatedAt", user.getUpdatedAt());
    userData.put("birthDate", user.getBirthDate());
    userData.put("country", user.getCountry());
    userData.put("postalCode", user.getPostalCode());

    return userData;
  }
}