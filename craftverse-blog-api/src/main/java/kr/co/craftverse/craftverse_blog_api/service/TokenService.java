package kr.co.craftverse.craftverse_blog_api.service;

import java.time.Duration;
import java.time.Instant;
import kr.co.craftverse.craftverse_blog_api.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {
  private final TokenRepository tokenRepository;
  private final OAuth2AuthorizedClientService authorizedClientService;
  private final ClientRegistrationRepository clientRegistrationRepository;

  /**
   * 현재 인증된 사용자의 액세스 토큰을 가져옵니다.
   * @param userId 사용자 ID
   * @return 액세스 토큰
   */
  public String getAccessToken(Long userId) {
    return tokenRepository.getAccessToken(userId);
  }

  /**
   * 토큰 갱신이 필요한지 확인합니다.
   * @param userId 사용자 ID
   * @return 갱신 필요 여부
   */
  public boolean isTokenRefreshRequired(Long userId) {
    Long expiration = tokenRepository.getAccessTokenExpiration(userId);
    // 유효 시간이 5분 미만으로 남았으면 갱신 필요
    return expiration != null && expiration < 300;
  }

  /**
   * OAuth 클라이언트를 통해 토큰을 갱신합니다.
   * 이 메서드는 실제 구현에서 OAuth2 프로바이더에 맞게 구현되어야 합니다.
   * @param auth OAuth 인증 토큰
   * @param userId 사용자 ID
   * @return 갱신 성공 여부
   */
  public boolean refreshToken(OAuth2AuthenticationToken auth, Long userId) {
    try {
      String clientRegistrationId = auth.getAuthorizedClientRegistrationId();
      String principalName = auth.getName();

      OAuth2AuthorizedClient authorizedClient =
          authorizedClientService.loadAuthorizedClient(clientRegistrationId, principalName);

      if (authorizedClient == null) {
        return false;
      }

      OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
      if (refreshToken == null) {
        return false;
      }

      // 여기서 OAuth 프로바이더에 따른 토큰 갱신 로직 구현
      // (Google, Facebook 등에 따라 다를 수 있음)

      // 토큰 갱신 성공 후 새 토큰 저장
      OAuth2AccessToken newAccessToken = authorizedClient.getAccessToken();
      long expiresIn = Duration.between(Instant.now(), newAccessToken.getExpiresAt()).getSeconds();

      tokenRepository.saveAccessToken(userId, newAccessToken.getTokenValue(), expiresIn);

      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 사용자 로그아웃 시 토큰 삭제
   * @param userId 사용자 ID
   */
  public void removeTokens(Long userId) {
    tokenRepository.deleteTokens(userId);
  }
}
