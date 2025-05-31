package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.model.dto.GoogleLoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.LoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserRegistrationRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserResponseDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.VerifyEmailDTO;
import kr.co.craftverse.craftverse_blog_api.service.EmailVerificationService;
import kr.co.craftverse.craftverse_blog_api.service.OAuth2Service;
import kr.co.craftverse.craftverse_blog_api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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

  // ========== 일반 회원가입/로그인 ==========

  /**
   * 일반 회원가입
   */
  @PostMapping("/register")
  public RestResult<Map<String, Object>> registerUser(
      @Valid @RequestBody UserRegistrationRequestDTO userRegistrationRequestDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    UserResponseDTO userResponseDTO = userService.registerUser(userRegistrationRequestDTO);
    data.put("user", userResponseDTO);
    return new RestResult<>(data);
  }

  /**
   * 일반 로그인
   */
  @PostMapping("/login")
  public RestResult<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    String accessToken = userService.login(loginRequestDTO);
    data.put("accessToken", accessToken);
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

  // ========== 구글 소셜 로그인 ==========

  /**
   * 구글 로그인/회원가입 URL을 반환합니다.
   * @param action "login" 또는 "signup"
   */
  @GetMapping("/google/url")
  public RestResult<Map<String, Object>> getGoogleAuthUrl(
      @RequestParam(value = "action", defaultValue = "login") String action) {

    Map<String, Object> data = new LinkedHashMap<>();

    // action 파라미터 검증
    if (!"login".equals(action) && !"signup".equals(action)) {
      action = "login"; // 기본값으로 설정
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
  public RestResult<Map<String, Object>> googleLogin(@RequestBody GoogleLoginRequestDTO requestDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    Map<String, String> tokenInfo = oAuth2Service.loginWithGoogle(requestDTO.getCode());

    data.put("accessToken", tokenInfo.get("accessToken"));
    data.put("user", tokenInfo.get("user"));
    return new RestResult<>(data);
  }

  // ========== 공통 인증 기능 ==========

  /**
   * 토큰 갱신 API
   */
  @PostMapping("/refresh")
  public RestResult<Map<String, Object>> refreshToken(
      @RequestParam("userId") Long userId,
      @RequestParam("email") String email,
      @RequestParam("refreshToken") String refreshToken) {

    Map<String, Object> data = new LinkedHashMap<>();
    String newAccessToken = oAuth2Service.refreshAccessToken(userId, email, refreshToken);

    data.put("accessToken", newAccessToken);
    return new RestResult<>(data);
  }

  @PostMapping("/logout")
  public RestResult<Map<String, Object>> logout(HttpServletRequest request) {
    Map<String, Object> data = new LinkedHashMap<>();

    // 일반 로그아웃과 소셜 로그아웃을 모두 처리
    userService.logout(request);
    oAuth2Service.logout(request);

    data.put("message", "로그아웃되었습니다.");
    return new RestResult<>(data);
  }

  // ========== Private Helper Methods ==========

  /**
   * state 파라미터에서 action 정보를 추출합니다.
   * state 형식: "action=login" 또는 "action=signup"
   */
  private String extractActionFromState(String state) {
    if (state != null && state.startsWith("action=")) {
      String action = state.substring(7); // "action=" 제거
      if ("login".equals(action) || "signup".equals(action)) {
        return action;
      }
    }
    return "login"; // 기본값
  }
}