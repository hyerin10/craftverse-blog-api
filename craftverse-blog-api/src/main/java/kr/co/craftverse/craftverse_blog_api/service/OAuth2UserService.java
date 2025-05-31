package kr.co.craftverse.craftverse_blog_api.service;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.TokenRepository;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;
  private final TokenRepository tokenRepository;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oauth2User = super.loadUser(userRequest);

    try {
      return processOAuth2User(userRequest, oauth2User);
    } catch (Exception e) {
      throw new InternalAuthenticationServiceException(e.getMessage(), e);
    }
  }

  private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
    // Google OAuth 속성 추출
    String provider = userRequest.getClientRegistration().getRegistrationId();
    String oauthId = oauth2User.getAttribute("sub");
    String email = oauth2User.getAttribute("email");
    String firstName = oauth2User.getAttribute("given_name");
    String lastName = oauth2User.getAttribute("family_name");
    String pictureUrl = oauth2User.getAttribute("picture");

    // 액세스 토큰 정보
    String accessToken = userRequest.getAccessToken().getTokenValue();
    long expirationTime = ChronoUnit.SECONDS.between(
        Instant.now(),
        userRequest.getAccessToken().getExpiresAt()
    );

    Optional<User> userOptional = userRepository.findByEmail(email);

    User user;
    if (userOptional.isPresent()) {
      // 기존 사용자 업데이트
      user = userOptional.get();
      user.updateOAuthInfo(provider, oauthId, pictureUrl);
    } else {
      // 새 사용자 생성
      long currentTime = Instant.now().getEpochSecond();
      user = User.builder()
          .email(email)
          .firstName(firstName)
          .lastName(lastName)
          .emailVerified(true)
          .oauthProvider(provider)
          .oauthId(oauthId)
          .profilePictureUrl(pictureUrl)
          .createdAt(currentTime)
          .updatedAt(currentTime)
          .lastLogin(currentTime)
          .loginAttempts(0)
          .accountLocked(false)
          .build();
    }

    user = userRepository.save(user);

    // Redis에 토큰 저장
    tokenRepository.saveAccessToken(user.getId(), accessToken, expirationTime);

    // 리프레시 토큰이 있으면 저장 (Google의 경우 별도 요청 필요)
    // 토큰 갱신 관련 로직이 추가될 수 있음

    return oauth2User;
  }
}