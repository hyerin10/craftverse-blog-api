package kr.co.craftverse.craftverse_blog_api.controller;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.ACTION_LOGIN;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.ACTION_SIGNUP;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.OAUTH_ERROR_ACCESS_DENIED;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.STATE_PREFIX_ACTION;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.GoogleLoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.LoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.OAuthUserUpdateRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.ResetPasswordRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.UserRegistrationRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.UserResponseDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.UserUpdateRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.VerifyEmailDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.auth.VerifyPasswordResetDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.service.EmailVerificationService;
import kr.co.craftverse.craftverse_blog_api.service.OAuth2Service;
import kr.co.craftverse.craftverse_blog_api.service.PasswordResetService;
import kr.co.craftverse.craftverse_blog_api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

  private final UserService userService;
  private final OAuth2Service oAuth2Service;
  private final EmailVerificationService emailVerificationService;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordResetService passwordResetService;

  /**
   * 현재 사용자 정보 조회 API
   */
  @GetMapping("/me")
  public RestResult<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
    Map<String, Object> data = new LinkedHashMap<>();

    String token = jwtTokenProvider.resolveToken(request);
    if (token == null || !jwtTokenProvider.validateToken(token))
      throw new UnauthorizedException();

    Long userId = jwtTokenProvider.getUserId(token);
    UserResponseDTO user = userService.getCurrentUser(userId);

    data.put("user", user);
    return new RestResult<>(data);
  }

  /**
   * 사용자 정보 부분 수정 API (PATCH)
   */
  @PatchMapping("/user")
  public RestResult<Map<String, Object>> patchUser(
      @RequestBody UserUpdateRequestDTO userUpdateRequestDTO, HttpServletRequest request) {

    Map<String, Object> data = new LinkedHashMap<>();

    String token = jwtTokenProvider.resolveToken(request);
    if (token == null || !jwtTokenProvider.validateToken(token))
      throw new UnauthorizedException();

    Long userId = jwtTokenProvider.getUserId(token);

    // 부분 업데이트 처리
    UserResponseDTO updatedUser = userService.patchUser(userId, userUpdateRequestDTO);

    data.put("user", updatedUser);

    return new RestResult<>(data);
  }

  /**
   * 사용자 정보 부분 수정 API (PATCH) - OAuth
   */
  @PatchMapping("/user/oauth")
  public RestResult<Map<String, Object>> patchUser(
      @RequestBody OAuthUserUpdateRequestDTO oAuthUserUpdateRequestDTO, HttpServletRequest request) {

    Map<String, Object> data = new LinkedHashMap<>();

    String token = jwtTokenProvider.resolveToken(request);
    if (token == null || !jwtTokenProvider.validateToken(token))
      throw new UnauthorizedException();

    Long userId = jwtTokenProvider.getUserId(token);

    // 부분 업데이트 처리
    UserResponseDTO updatedUser = userService.patchUser(userId, oAuthUserUpdateRequestDTO);

    data.put("user", updatedUser);

    return new RestResult<>(data);
  }

  /**
   * 간단한 토큰 검증 API (사용자 정보 없이)
   */
  @PostMapping("/token/status")
  public RestResult<Map<String, Object>> validateTokenOnly(HttpServletRequest request) {
    Map<String, Object> data = new LinkedHashMap<>();

    String token = jwtTokenProvider.resolveToken(request);
    boolean isValid = token != null && jwtTokenProvider.validateToken(token);

    data.put("valid", isValid);

    if (isValid) {
      Long userId = jwtTokenProvider.getUserId(token);
      String email = jwtTokenProvider.getEmail(token);
      data.put("tokenInfo", Map.of(
          "userId", userId,
          "email", email
      ));
    }

    return new RestResult<>(data);
  }

  // ========== 일반 회원가입/로그인 ==========

  /**
   * 일반 회원가입
   */
  @PostMapping("/user")
  public RestResult<Map<String, Object>> registerUser(
      @Valid @RequestBody UserRegistrationRequestDTO userRegistrationRequestDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    UserResponseDTO userResponseDTO = userService.registerUser(userRegistrationRequestDTO);
    data.put("user", userResponseDTO);
    return new RestResult<>(data);
  }

  /**
   * 일반 로그인 (수정: 리프레시 토큰 포함)
   */
  @PostMapping("/login")
  public RestResult<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    Map<String, String> tokens = userService.login(loginRequestDTO);
    data.put("accessToken", tokens.get("accessToken"));
    data.put("refreshToken", tokens.get("refreshToken"));
    return new RestResult<>(data);
  }

  /**
   * 이메일 인증
   */
  @PostMapping("/verify-email")
  public RestResult<Map<String, Object>> verifyEmail(@Valid @RequestBody VerifyEmailDTO verifyEmailDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    boolean verified = emailVerificationService.verifyEmail(verifyEmailDTO);
    data.put("verified", verified);
    return new RestResult<>(data);
  }

  @PostMapping("/resend-verification")
  public RestResult<Map<String, Object>> resendVerificationEmail(
      @RequestParam("email") String email) {

    Map<String, Object> data = new LinkedHashMap<>();

    // UserService의 resendVerificationEmail 메서드 호출
    userService.resendVerificationEmail(email);

    data.put("message", "인증 코드가 재전송되었습니다.");
    return new RestResult<>(data);
  }

  // ========== 구글 소셜 로그인 ==========

  /**
   * 구글 로그인/회원가입 URL을 반환합니다.
   * @param action "login" 또는 "signup"
   */
  @GetMapping("/google/url")
  public RestResult<Map<String, Object>> getGoogleAuthUrl(
      @RequestParam(value = "action", defaultValue = ACTION_LOGIN) String action) {

    Map<String, Object> data = new LinkedHashMap<>();

    // action 파라미터 검증
    if (!ACTION_LOGIN.equals(action) && !ACTION_SIGNUP.equals(action)) {
      action = ACTION_LOGIN; // 기본값으로 설정
    }

    String authUrl = oAuth2Service.getGoogleAuthUrl(action);
    data.put("authUrl", authUrl);
    data.put("action", action);

    return new RestResult<>(data);
  }

  /**
   * 구글 로그인/회원가입 콜백을 처리합니다.
   */
  @GetMapping("/google/callback")
  public ResponseEntity<Void> googleCallback(
      @RequestParam("code") String code,
      @RequestParam(value = "state", required = false) String state,
      HttpServletRequest request,
      HttpServletResponse response) throws Exception {

    // state 파라미터에서 action 정보 추출 (로그인/회원가입 구분)
    String action = extractActionFromState(state);

    // 구글 인증 코드로 로그인/회원가입 처리 및 리다이렉트
    String redirectUrl = oAuth2Service.processGoogleCallback(code, action, request, response);

    return ResponseEntity.status(HttpStatus.FOUND)
        .header("Location", redirectUrl)
        .build();
  }

  /**
   * 구글 로그인 API (모바일/프론트엔드에서 획득한 코드로 로그인)
   */
  @PostMapping("/google/login")
  public RestResult<Map<String, Object>> googleLogin(@Valid @RequestBody GoogleLoginRequestDTO googleLoginRequestDTO) {
    Map<String, Object> data = new LinkedHashMap<>();

    Map<String, String> tokenInfo = oAuth2Service.loginWithGoogle(googleLoginRequestDTO.getCode());

    // 사용자가 로그인을 거부한 경우
    if (OAUTH_ERROR_ACCESS_DENIED.equals(googleLoginRequestDTO.getError()))
      throw new UnauthorizedException();

    if (googleLoginRequestDTO.getCode() == null || googleLoginRequestDTO.getCode().trim().isEmpty())
      throw new UnauthorizedException();

    data.put("accessToken", tokenInfo.get("accessToken"));
    data.put("refreshToken", tokenInfo.get("refreshToken"));
    data.put("user", tokenInfo.get("user"));
    return new RestResult<>(data);
  }

  // ========== 공통 인증 기능 ==========

  /**
   * 토큰 갱신 API (RTR 적용 - 일반/OAuth 분기처리)
   */
  @PostMapping("/refresh")
  public RestResult<Map<String, Object>> refreshToken(
      @RequestParam("userId") Long userId,
      @RequestParam("email") String email,
      @RequestParam("refreshToken") String refreshToken) {

    Map<String, Object> data = new LinkedHashMap<>();

    // 사용자 정보 조회로 OAuth 여부 확인
    User user = userService.findById(userId);

    Map<String, String> tokens;

    if (user.getOauthProvider() != null) {
      // OAuth 사용자인 경우 - 구글 리프레시 토큰 로직 사용
      tokens = oAuth2Service.refreshAccessToken(userId, email, refreshToken);
    } else {
      // 일반 사용자인 경우 - 일반 리프레시 토큰 로직 사용
      tokens = userService.refreshAccessToken(userId, email, refreshToken);
    }

    data.put("accessToken", tokens.get("accessToken"));
    data.put("refreshToken", tokens.get("refreshToken"));
    return new RestResult<>(data);
  }

  @PostMapping("/logout")
  public RestResult<Map<String, Object>> logout(HttpServletRequest request) {
    Map<String, Object> data = new LinkedHashMap<>();
    userService.logout(request);
    data.put("message", "로그아웃되었습니다.");
    return new RestResult<>(data);
  }

  @DeleteMapping("/user")
  public RestResult<Map<String, Object>> delete(HttpServletRequest request) {
    Map<String, Object> data = new LinkedHashMap<>();

    String token = jwtTokenProvider.resolveToken(request);
    if (token == null || !jwtTokenProvider.validateToken(token))
      throw new UnauthorizedException();

    Long userId = jwtTokenProvider.getUserId(token);
    userService.delete(userId);
    jwtTokenProvider.blacklistToken(token);

    data.put("message", "success");
    return new RestResult<>(data);
  }

  // ========== Private Helper Methods ==========

  /**
   * state 파라미터에서 action 정보를 추출합니다.
   * state 형식: "action=login" 또는 "action=signup"
   */
  private String extractActionFromState(String state) {
    if (state != null && state.startsWith(STATE_PREFIX_ACTION + ACTION_SIGNUP))
      return ACTION_SIGNUP;
    return ACTION_LOGIN;
  }

  // ========== 비밀번호 재설정 ==========

  /**
   * 비밀번호 재설정 인증 코드 발송
   */
  @PostMapping("/password-reset/send-code")
  public RestResult<Map<String, Object>> sendPasswordResetCode(
      @RequestParam("email") String email) {

    Map<String, Object> data = new LinkedHashMap<>();

    passwordResetService.sendPasswordResetCode(email);

    data.put("message", "비밀번호 재설정 인증 코드가 발송되었습니다.");
    return new RestResult<>(data);
  }

  /**
   * 비밀번호 재설정 인증 코드 검증
   */
  @PostMapping("/password-reset/verify-code")
  public RestResult<Map<String, Object>> verifyPasswordResetCode(
      @Valid @RequestBody VerifyPasswordResetDTO verifyPasswordResetDTO) {

    Map<String, Object> data = new LinkedHashMap<>();

    boolean verified = passwordResetService.verifyPasswordResetCode(verifyPasswordResetDTO);

    data.put("verified", verified);
    data.put("message", "인증 코드가 확인되었습니다.");
    return new RestResult<>(data);
  }

  /**
   * 비밀번호 재설정 완료 (검증된 코드로 새 비밀번호 설정)
   */
  @PostMapping("/password-reset/reset")
  public RestResult<Map<String, Object>> resetPassword(
      @Valid @RequestBody ResetPasswordRequestDTO resetPasswordRequestDTO) {

    Map<String, Object> data = new LinkedHashMap<>();

    // 비밀번호 재설정 처리
    passwordResetService.resetPassword(resetPasswordRequestDTO);

    data.put("message", "비밀번호가 성공적으로 변경되었습니다.");
    return new RestResult<>(data);
  }
}