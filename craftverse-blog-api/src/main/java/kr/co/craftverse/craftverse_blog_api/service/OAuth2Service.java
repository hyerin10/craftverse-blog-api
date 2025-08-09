package kr.co.craftverse.craftverse_blog_api.service;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.TokenRepository;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
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
  private final Logger logger;

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
  public String getGoogleAuthUrl(String action) {
    // state 파라미터에 action 정보를 포함
    String state = STATE_PREFIX_ACTION + action;

    return UriComponentsBuilder.fromHttpUrl(GOGGLE_OAUTH_BASE_URL)
        .queryParam(OAUTH_PARAM_CLIENT_ID, googleClientId)
        .queryParam(OAUTH_PARAM_REDIRECT_URI, redirectUri)
        .queryParam(OAUTH_PARAM_RESPONSE_TYPE, OAUTH_RESPONSE_TYPE_CODE)
        .queryParam(OAUTH_PARAM_SCOPE, OAUTH_SCOPE_PROFILE_EMAIL)
        .queryParam(OAUTH_PARAM_ACCESS_TYPE, OAUTH_ACCESS_TYPE_OFFLINE)  // 리프레시 토큰을 위해 필요
        .queryParam(OAUTH_PARAM_PROMPT, OAUTH_PROMPT_CONSENT)       // 매번 동의 화면 표시
        .queryParam(OAUTH_PARAM_STATE, state)            // action 정보 전달
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
    String accessToken = (String) tokenResponse.get(OAUTH_RESPONSE_ACCESS_TOKEN);
    String refreshToken = (String) tokenResponse.get(OAUTH_RESPONSE_REFRESH_TOKEN);
    Long expiresIn = ((Integer) tokenResponse.get(OAUTH_RESPONSE_EXPIRES_IN)).longValue();

    // 구글 사용자 정보 얻기
    Map<String, Object> userInfo = getGoogleUserInfo(accessToken);
    String email = (String) userInfo.get(USER_INFO_EMAIL);

    // 기존 사용자 확인
    Optional<User> existingUser = userRepository.findByEmail(email);

    User user;
    String resultAction;

    if (ACTION_SIGNUP.equals(action)) {
      // 회원가입 시도
      if (existingUser.isPresent()) {
        // 이미 존재하는 사용자 - 로그인으로 처리하되 알림
        user = existingUser.get();
        user.updateOAuthInfo(OAUTH_PROVIDER_GOOGLE, (String) userInfo.get(USER_INFO_SUB), (String) userInfo.get(USER_INFO_PICTURE));
        user = userRepository.save(user);
        resultAction = OAUTH_RESULT_LOGIN_EXISTING;
      } else {
        // 새 사용자 생성 (회원가입)
        user = createNewUser(userInfo, accessToken, refreshToken, expiresIn);
        resultAction = OAUTH_RESULT_SIGNUP_SUCCESS;
      }
    } else {
      // 로그인 시도
      if (existingUser.isPresent()) {
        // 기존 사용자 로그인
        user = existingUser.get();
        user.updateOAuthInfo(OAUTH_PROVIDER_GOOGLE, (String) userInfo.get(USER_INFO_SUB), (String) userInfo.get(USER_INFO_PICTURE));
        user = userRepository.save(user);
        resultAction = OAUTH_RESULT_LOGIN_SUCCESS;
      } else {
        // 사용자가 없음 - 회원가입으로 처리
        user = createNewUser(userInfo, accessToken, refreshToken, expiresIn);
        resultAction = OAUTH_RESULT_SIGNUP_AUTO;
      }
    }

    // JWT 토큰 생성
    String jwtToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());

    // 쿠키 설정
    setCookies(response, jwtToken);

    // 리다이렉트 URL 생성
    String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
        .queryParam(QUERY_PARAM_LOGIN, QUERY_PARAM_SUCCESS)
        .queryParam(QUERY_PARAM_USER_ID, user.getId())
        .queryParam(QUERY_PARAM_ACTION, resultAction)
        .queryParam(QUERY_PARAM_TOKEN, jwtToken)
        .toUriString();

    System.out.println("🔧 리다이렉트 URL: " + redirectUrl);

    return redirectUrl;
  }

  /**
   * 새 사용자를 생성합니다.
   */
  @Transactional
  public User createNewUser(Map<String, Object> userInfo, String accessToken, String refreshToken, Long expiresIn) {
    String email = (String) userInfo.get(USER_INFO_EMAIL);
    String sub = (String) userInfo.get(USER_INFO_SUB);
    String name = (String) userInfo.get(USER_INFO_NAME);
    String givenName = (String) userInfo.get(USER_INFO_GIVEN_NAME);
    String familyName = (String) userInfo.get(USER_INFO_FAMILY_NAME);
    String picture = (String) userInfo.get(USER_INFO_PICTURE);

    long currentTime = Instant.now().getEpochSecond();

    User user = User.builder()
        .email(email)
        .firstName(givenName != null ? givenName : name)
        .lastName(familyName)
        .emailVerified(true)
        .oauthProvider(OAUTH_PROVIDER_GOOGLE)
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
    if (refreshToken != null)
      tokenRepository.saveRefreshToken(user.getId(), refreshToken, REFRESH_TOKEN_EXPIRY_SECONDS); // 리프레시 토큰은 (30일)

    return user;
  }

  /**
   * 쿠키를 설정합니다.
   */
  private void setCookies(HttpServletResponse response, String jwtToken) {
    // 방법 1: 기본 쿠키
    Cookie authCookie = new Cookie(COOKIE_AUTH_TOKEN, jwtToken);
    authCookie.setPath(COOKIE_PATH_ROOT);
    authCookie.setHttpOnly(true);
    authCookie.setMaxAge((int) (tokenExpirationMs / 1000));
    response.addCookie(authCookie);

    // 방법 2: ResponseCookie로 직접 헤더 설정
    response.addHeader("Set-Cookie",
        String.format("%s=%s; Path=%s; HttpOnly; Max-Age=%d; SameSite=%s",
            COOKIE_AUTH_TOKEN, jwtToken, COOKIE_PATH_ROOT, (int) (tokenExpirationMs / 1000), COOKIE_SAME_SITE_LAX));

    // 방법 3: 일반 쿠키로도 설정 (테스트용)
    Cookie testCookie = new Cookie(COOKIE_TEST_AUTH_TOKEN, jwtToken);
    testCookie.setPath(COOKIE_PATH_ROOT);
    testCookie.setMaxAge((int) (tokenExpirationMs / 1000));
    response.addCookie(testCookie);

    logger.info("쿠키 설정 완료 - 토큰 길이: " + jwtToken.length());
  }

  /**
   * 구글 액세스 토큰 및 리프레시 토큰을 얻습니다.
   */
  private Map<String, Object> getGoogleTokens(String code) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add(OAUTH_PARAM_CODE, code);
    body.add(OAUTH_PARAM_CLIENT_ID, googleClientId);
    body.add(OAUTH_PARAM_CLIENT_SECRET, googleClientSecret);
    body.add(OAUTH_PARAM_REDIRECT_URI, redirectUri);
    body.add(OAUTH_PARAM_GRANT_TYPE, GRANT_TYPE_AUTHORIZATION_CODE);

    HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

    ResponseEntity<Map> response = restTemplate.exchange(
        GOOGLE_TOKEN_URL,
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
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    HttpEntity<?> entity = new HttpEntity<>(headers);

    ResponseEntity<Map> response = restTemplate.exchange(
        GOOGLE_USERINFO_URL,
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
    // 구글에서 액세스 토큰 얻기
    Map<String, Object> tokenResponse = getGoogleTokens(code);
    String accessToken = (String) tokenResponse.get(OAUTH_RESPONSE_ACCESS_TOKEN);
    String refreshToken = (String) tokenResponse.get(OAUTH_RESPONSE_REFRESH_TOKEN);
    Long expiresIn = ((Integer) tokenResponse.get(OAUTH_RESPONSE_EXPIRES_IN)).longValue();

    // 구글 사용자 정보 얻기
    Map<String, Object> userInfo = getGoogleUserInfo(accessToken);

    // 사용자 정보로 로그인 처리 (기존 사용자 또는 새 사용자 자동 생성)
    User user = processUserLogin(userInfo, accessToken, refreshToken, expiresIn);

    // JWT 토큰 생성
    String jwtToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());

    Map<String, String> result = new HashMap<>();
    result.put(OAUTH_RESPONSE_ACCESS_TOKEN, jwtToken);
    result.put(OAUTH_RESPONSE_REFRESH_TOKEN, refreshToken);
    try{
      result.put("user", objectMapper.writeValueAsString(user));
    } catch (JsonProcessingException e) {
      logger.error("JsonProcessingException: "+e.getMessage());
    }
    return result;
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

    String email = (String) userInfo.get(USER_INFO_EMAIL);
    String sub = (String) userInfo.get(USER_INFO_SUB);
    String name = (String) userInfo.get(USER_INFO_NAME);
    String givenName = (String) userInfo.get(USER_INFO_GIVEN_NAME);
    String familyName = (String) userInfo.get(USER_INFO_FAMILY_NAME);
    String picture = (String) userInfo.get(USER_INFO_PICTURE);

    // 이메일로 사용자 찾기
    Optional<User> userOptional = userRepository.findByEmail(email);

    User user;

    if (userOptional.isPresent()) {
      // 기존 사용자 업데이트
      user = userOptional.get();
      user.updateOAuthInfo(OAUTH_PROVIDER_GOOGLE, sub, picture);
    } else {
      // 새 사용자 생성
      long currentTime = Instant.now().getEpochSecond();
      user = User.builder()
          .email(email)
          .firstName(givenName != null ? givenName : name)
          .lastName(familyName)
          .emailVerified(true)
          .oauthProvider(OAUTH_PROVIDER_GOOGLE)
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
    if (refreshToken != null)
      tokenRepository.saveRefreshToken(user.getId(), refreshToken, REFRESH_TOKEN_EXPIRY_SECONDS); // 리프레시 토큰 (30일)

    return user;
  }

  /**
   * 리프레시 토큰을 사용하여 액세스 토큰을 갱신합니다. (RTR 적용)
   */
  @Transactional
  public Map<String, String> refreshAccessToken(Long userId, String email, String refreshToken) {
    // Redis에서 리프레시 토큰 확인
    String storedRefreshToken = tokenRepository.getRefreshToken(userId);

    if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken))
      throw new UnauthorizedException();

    // 사용자 존재 여부 확인
    User user = userRepository.findByEmail(email)
        .orElseThrow(UnauthorizedException::new);

    // 기존 리프레시 토큰 삭제 (RTR)
    tokenRepository.deleteRefreshToken(userId);

    // 구글 API로 액세스 토큰 갱신
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add(OAUTH_PARAM_CLIENT_ID, googleClientId);
    body.add(OAUTH_PARAM_CLIENT_SECRET, googleClientSecret);
    body.add(OAUTH_PARAM_REFRESH_TOKEN, refreshToken);
    body.add(OAUTH_PARAM_GRANT_TYPE, GRANT_TYPE_REFRESH_TOKEN);

    HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

    ResponseEntity<JsonNode> response = restTemplate.exchange(
        GOOGLE_TOKEN_URL,
        HttpMethod.POST,
        entity,
        JsonNode.class
    );

    JsonNode tokenResponse = response.getBody();
    String newGoogleAccessToken = tokenResponse.get(OAUTH_RESPONSE_ACCESS_TOKEN).asText();
    int expiresIn = tokenResponse.get(OAUTH_RESPONSE_EXPIRES_IN).asInt();

    // 새로운 리프레시 토큰이 있으면 사용, 없으면 기존 것 재사용
    String newGoogleRefreshToken = tokenResponse.has(OAUTH_RESPONSE_REFRESH_TOKEN)
        ? tokenResponse.get(OAUTH_RESPONSE_REFRESH_TOKEN).asText()
        : refreshToken;

    // Redis에 새 토큰들 저장
    tokenRepository.saveAccessToken(userId, newGoogleAccessToken, expiresIn);
    tokenRepository.saveRefreshToken(userId, newGoogleRefreshToken, REFRESH_TOKEN_EXPIRY_SECONDS);

    // JWT 토큰 새로 생성
    String newJwtAccessToken = jwtTokenProvider.createAccessToken(userId, email);

    Map<String, String> tokens = new HashMap<>();
    tokens.put("accessToken", newJwtAccessToken);
    tokens.put("refreshToken", newGoogleRefreshToken);

    logger.info("[OAuth2Service] OAuth 사용자 토큰 갱신 완료: userId={}, email={}", userId, email);

    return tokens;
  }

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
      logger.info("[OAuth2Service] 로그아웃 완료: userId={}, email={}", userId, email);
    } else {
      logger.warn("[OAuth2Service] 유효하지 않은 토큰으로 로그아웃 시도");
    }
  }
}