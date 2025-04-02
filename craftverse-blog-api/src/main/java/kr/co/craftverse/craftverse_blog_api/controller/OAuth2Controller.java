package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.model.dto.GoogleLoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.service.OAuth2Service;
import kr.co.craftverse.craftverse_blog_api.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OAuth2Controller {

  private final OAuth2Service oAuth2Service;
  private final TokenService tokenService;

  /**
   * 구글 로그인 URL을 반환합니다.
   */
  @GetMapping("/google/url")
  public RestResult<Map<String, Object>> getGoogleAuthUrl(HttpServletRequest request) {
    Map<String, Object> data = new LinkedHashMap<>();
    String authUrl = oAuth2Service.getGoogleAuthUrl(request);
    data.put("authUrl", authUrl);
    return new RestResult<>(data);
  }

  /**
   * 구글 로그인 콜백을 처리합니다.
   */
  @GetMapping("/google/callback")
  public ResponseEntity<Void> googleCallback(
      @RequestParam("code") String code,
      HttpServletRequest request,
      HttpServletResponse response) throws Exception {

    // 구글 인증 코드로 로그인 처리 및 리다이렉트
    String redirectUrl = oAuth2Service.processGoogleCallback(code, request, response);
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

  /**
   * 로그아웃 API
   */
  @PostMapping("/logout")
  public RestResult<Map<String, Object>> logout(@RequestParam("userId") Long userId) {
    Map<String, Object> data = new LinkedHashMap<>();
    tokenService.removeTokens(userId);

    data.put("message", "로그아웃되었습니다.");
    return new RestResult<>(data);
  }
}
