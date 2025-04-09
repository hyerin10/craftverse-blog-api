package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.model.dto.GoogleLoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.LoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserRegistrationRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserResponseDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.VerifyEmailDTO;
import kr.co.craftverse.craftverse_blog_api.service.AuthService;
import kr.co.craftverse.craftverse_blog_api.service.OAuth2Service;
import kr.co.craftverse.craftverse_blog_api.service.UserService;
import kr.co.craftverse.craftverse_blog_api.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

  // Google OAuth 로그인 URL을 가져오는 API
  @GetMapping("/auth/google")
  public RestResult<Map<String, Object>> getGoogleAuthUrl(HttpServletRequest request) {
    Map<String, Object> data = new LinkedHashMap<>();
    String authUrl = oAuth2Service.getGoogleAuthUrl(request);
    data.put("authUrl", authUrl);
    return new RestResult<>(data);
  }

  // Google OAuth 로그인 API (프론트엔드에서 받은 코드로 로그인)
  @PostMapping("/auth/google")
  public RestResult<Map<String, Object>> googleLogin(@Valid @RequestBody GoogleLoginRequestDTO requestDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    Map<String, String> tokenInfo = oAuth2Service.loginWithGoogle(requestDTO.getCode());

    data.put("accessToken", tokenInfo.get("accessToken"));
    data.put("user", tokenInfo.get("user"));
    return new RestResult<>(data);
  }

  @PostMapping("/logout")
  public RestResult<Map<String, Object>> logout(HttpServletRequest request) {
    Map<String, Object> data = new LinkedHashMap<>();
    String token = jwtTokenProvider.resolveToken(request);

    if (token != null) {
      jwtTokenProvider.blacklistToken(token);
      data.put("message", "로그아웃 되었습니다.");
    } else
      data.put("message", "인증 토큰이 없습니다.");

    return new RestResult<>(data);
  }
}