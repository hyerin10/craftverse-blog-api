package kr.co.craftverse.craftverse_blog_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.TokenRepository;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class OAuth2Service {

  private final RestTemplate restTemplate = new RestTemplate();
  private final UserRepository userRepository;
  private final TokenRepository tokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${spring.security.oauth2.client.registration.google.client-id}")
  private String googleClientId;

  @Value("${spring.security.oauth2.client.registration.google.client-secret}")
  private String googleClientSecret;

  @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
  private String redirectUri;

  @Value("${app.auth.frontend-redirect-uri}")
  private String frontendRedirectUri;

  @Value("${app.auth.token-secret}")
  private String tokenSecret;

  @Value("${app.auth.token-expiration-ms}")
  private long tokenExpirationMs;

  /**
   * 구글 로그인/회원가입 URL을 생성합니다.
   * @param action "login" 또는 "signup"
   */
  public String getGoogleAuthUrl(HttpServletRequest request, String action) {
    String baseUrl = "https://accounts.google.com/o/oauth2/v2/auth";

    // state 파라미터에 action 정보를 포함
    String state = "action=" + action;

    return UriComponentsBuilder.fromHttpUrl(baseUrl)
        .queryParam("client_id", googleClientId)
        .queryParam("redirect_uri", redirectUri)
        .queryParam("response_type", "code")
        .queryParam("scope", "profile email")
        .queryParam("access_type", "offline")  // 리프레시 토큰을 위해 필요
        .queryParam("prompt", "consent")       // 매번 동의 화면 표시
        .queryParam("state", state)            // action 정보 전달
        .toUriString();
  }

  /**
   * 구글 로그인/회원가입 콜백을 처리합니다.
   * @param action "login" 또는 "signup"
   */
  @Transactional
  public String processGoogleCallback(String code, String action, HttpServletRequest request, HttpServletResponse response) throws Exception {
    // 구글에서 액세스 토큰 얻기
    Map<String, Object> tokenResponse = getGoogleTokens(code);
    String accessToken = (String) tokenResponse.get("access_token");
    String refreshToken = (String) tokenResponse.get("refresh_token");
    Long expiresIn = ((Integer) tokenResponse.get("expires_in")).longValue();

    // 구글 사용자 정보 얻기
    Map<String, Object> userInfo = getGoogleUserInfo(accessToken);
    String email = (String) userInfo.get("email");

    // 기존 사용자 확인
    Optional<User> existingUser = userRepository.findByEmail(email);

    User user;
    String resultAction;

    if ("signup".equals(action)) {
      // 회원가입 시도
      if (existingUser.isPresent()) {
        // 이미 존재하는 사용자 - 로그인으로 처리하되 알림
        user = existingUser.get();
        user.updateOAuthInfo("google", (String) userInfo.get("sub"), (String) userInfo.get("picture"));
        user = userRepository.save(user);
        resultAction = "login_existing";
      } else {
        // 새 사용자 생성 (회원가입)
        user = createNewUser(userInfo, accessToken, refreshToken, expiresIn);
        resultAction = "signup_success";
      }
    } else {
      // 로그인 시도
      if (existingUser.isPresent()) {
        // 기존 사용자 로그인
        user = existingUser.get();
        user.updateOAuthInfo("google", (String) userInfo.get("sub"), (String) userInfo.get("picture"));
        user = userRepository.save(user);
        resultAction = "login_success";
      } else {
        // 사용자가 없음 - 회원가입으로 처리
        user = createNewUser(userInfo, accessToken, refreshToken, expiresIn);
        resultAction = "signup_auto";
      }
    }

    // JWT 토큰 생성
    String jwtToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());

    // 쿠키 설정
    setCookies(response, jwtToken);

    // 리다이렉트 URL 생성
    String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
        .queryParam("login", "success")
        .queryParam("user_id", user.getId())
        .queryParam("action", resultAction)
        .queryParam("token", jwtToken)
        .toUriString();

    System.out.println("🔧 리다이렉트 URL: " + redirectUrl);

    return redirectUrl;
  }

  /**
   * 새 사용자를 생성합니다.
   */
  @Transactional
  private User createNewUser(Map<String, Object> userInfo, String accessToken, String refreshToken, Long expiresIn) {
    String email = (String) userInfo.get("email");
    String sub = (String) userInfo.get("sub");
    String name = (String) userInfo.get("name");
    String givenName = (String) userInfo.get("given_name");
    String familyName = (String) userInfo.get("family_name");
    String picture = (String) userInfo.get("picture");

    long currentTime = Instant.now().getEpochSecond();

    User user = User.builder()
        .email(email)
        .firstName(givenName != null ? givenName : name)
        .lastName(familyName)
        .emailVerified(true)
        .oauthProvider("google")
        .oauthId(sub)
        .profilePictureUrl(picture)
        .createdAt(currentTime)
        .updatedAt(currentTime)
        .lastLogin(currentTime)
        .loginAttempts(0)
        .accountLocked(false)
        .build();

    user = userRepository.save(user);

    // Redis에 토큰 저장
    tokenRepository.saveAccessToken(user.getId(), accessToken, expiresIn);
    if (refreshToken != null) {
      // 리프레시 토큰은 더 오래 보관 (30일)
      tokenRepository.saveRefreshToken(user.getId(), refreshToken, 30 * 24 * 60 * 60);
    }

    return user;
  }

  /**
   * 쿠키를 설정합니다.
   */
  private void setCookies(HttpServletResponse response, String jwtToken) {
    try {
      // 방법 1: 기본 쿠키
      Cookie authCookie = new Cookie("auth_token", jwtToken);
      authCookie.setPath("/");
      authCookie.setHttpOnly(true);
      authCookie.setMaxAge((int) (tokenExpirationMs / 1000));
      response.addCookie(authCookie);

      // 방법 2: ResponseCookie로 직접 헤더 설정
      response.addHeader("Set-Cookie",
          String.format("auth_token=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Lax",
              jwtToken, (int) (tokenExpirationMs / 1000)));

      // 방법 3: 일반 쿠키로도 설정 (테스트용)
      Cookie testCookie = new Cookie("test_auth_token", jwtToken);
      testCookie.setPath("/");
      testCookie.setMaxAge((int) (tokenExpirationMs / 1000));
      response.addCookie(testCookie);

      System.out.println("🔧 쿠키 설정 완료 - 토큰 길이: " + jwtToken.length());
    } catch (Exception e) {
      System.out.println("쿠키 설정 중 오류 발생: " + e);
    }
  }

  /**
   * 구글 액세스 토큰 및 리프레시 토큰을 얻습니다.
   */
  private Map<String, Object> getGoogleTokens(String code) {
    String tokenUrl = "https://oauth2.googleapis.com/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("code", code);
    body.add("client_id", googleClientId);
    body.add("client_secret", googleClientSecret);
    body.add("redirect_uri", redirectUri);
    body.add("grant_type", "authorization_code");

    HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

    ResponseEntity<Map> response = restTemplate.exchange(
        tokenUrl,
        HttpMethod.POST,
        entity,
        Map.class
    );

    return response.getBody();
  }

  /**
   * 구글 사용자 정보를 얻습니다.
   */
  private Map<String, Object> getGoogleUserInfo(String accessToken) {
    String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    HttpEntity<?> entity = new HttpEntity<>(headers);

    ResponseEntity<Map> response = restTemplate.exchange(
        userInfoUrl,
        HttpMethod.GET,
        entity,
        Map.class
    );

    return response.getBody();
  }

  /**
   * 모바일/프론트엔드에서 제공한 구글 코드로 로그인합니다.
   */
  @Transactional
  public Map<String, String> loginWithGoogle(String code) {
    try {
      // 구글에서 액세스 토큰 얻기
      Map<String, Object> tokenResponse = getGoogleTokens(code);
      String accessToken = (String) tokenResponse.get("access_token");
      String refreshToken = (String) tokenResponse.get("refresh_token");
      Long expiresIn = ((Integer) tokenResponse.get("expires_in")).longValue();

      // 구글 사용자 정보 얻기
      Map<String, Object> userInfo = getGoogleUserInfo(accessToken);

      // 사용자 정보로 로그인 처리 (기존 사용자 또는 새 사용자 자동 생성)
      User user = processUserLogin(userInfo, accessToken, refreshToken, expiresIn);

      // JWT 토큰 생성
      String jwtToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());

      Map<String, String> result = new HashMap<>();
      result.put("accessToken", jwtToken);
      result.put("user", objectMapper.writeValueAsString(user));

      return result;
    } catch (Exception e) {
      throw new RuntimeException("구글 로그인 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
  }

  /**
   * 사용자 정보를 사용하여 로그인 처리 또는 회원가입을 진행합니다.
   */
  @Transactional
  public User processUserLogin(
      Map<String, Object> userInfo,
      String accessToken,
      String refreshToken,
      Long expiresIn) {

    String email = (String) userInfo.get("email");
    String sub = (String) userInfo.get("sub");
    String name = (String) userInfo.get("name");
    String givenName = (String) userInfo.get("given_name");
    String familyName = (String) userInfo.get("family_name");
    String picture = (String) userInfo.get("picture");

    // 이메일로 사용자 찾기
    Optional<User> userOptional = userRepository.findByEmail(email);

    User user;

    if (userOptional.isPresent()) {
      // 기존 사용자 업데이트
      user = userOptional.get();
      user.updateOAuthInfo("google", sub, picture);
    } else {
      // 새 사용자 생성
      long currentTime = Instant.now().getEpochSecond();
      user = User.builder()
          .email(email)
          .firstName(givenName != null ? givenName : name)
          .lastName(familyName)
          .emailVerified(true)
          .oauthProvider("google")
          .oauthId(sub)
          .profilePictureUrl(picture)
          .createdAt(currentTime)
          .updatedAt(currentTime)
          .lastLogin(currentTime)
          .loginAttempts(0)
          .accountLocked(false)
          .build();
    }

    user = userRepository.save(user);

    // Redis에 토큰 저장
    tokenRepository.saveAccessToken(user.getId(), accessToken, expiresIn);
    if (refreshToken != null) {
      // 리프레시 토큰은 더 오래 보관 (30일)
      tokenRepository.saveRefreshToken(user.getId(), refreshToken, 30 * 24 * 60 * 60);
    }

    return user;
  }

  /**
   * 리프레시 토큰을 사용하여 액세스 토큰을 갱신합니다.
   */
  public String refreshAccessToken(Long userId, String email, String refreshToken) {
    try {
      // Redis에서 리프레시 토큰 확인
      String storedRefreshToken = tokenRepository.getRefreshToken(userId);

      if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
        throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
      }

      // 구글 API로 액세스 토큰 갱신
      String tokenUrl = "https://oauth2.googleapis.com/token";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("client_id", googleClientId);
      body.add("client_secret", googleClientSecret);
      body.add("refresh_token", refreshToken);
      body.add("grant_type", "refresh_token");

      HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

      ResponseEntity<JsonNode> response = restTemplate.exchange(
          tokenUrl,
          HttpMethod.POST,
          entity,
          JsonNode.class
      );

      JsonNode tokenResponse = response.getBody();
      String newAccessToken = tokenResponse.get("access_token").asText();
      int expiresIn = tokenResponse.get("expires_in").asInt();

      // Redis에 새 액세스 토큰 저장
      tokenRepository.saveAccessToken(userId, newAccessToken, expiresIn);

      // JWT 토큰 새로 생성
      return jwtTokenProvider.createAccessToken(userId, email);

    } catch (Exception e) {
      throw new RuntimeException("토큰 갱신 중 오류가 발생했습니다: " + e.getMessage(), e);
    }
  }
}