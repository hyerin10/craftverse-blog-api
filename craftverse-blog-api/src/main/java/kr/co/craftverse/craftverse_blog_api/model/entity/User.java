package kr.co.craftverse.craftverse_blog_api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.co.craftverse.craftverse_blog_api.model.dto.OAuthUserUpdateRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserUpdateRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name="users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name="id")
  private Long id; // long에서 Long으로 변경 (일관성을 위해)

  @Column(name="first_name")
  private String firstName;

  @Column(name="last_name")
  private String lastName;

  @Column(name="email")
  private String email;

  @Column(name="password_hash")
  private String password;

  @Column(name="birth_date")
  private Long birthDate;

  @Column(name="country")
  private String country;

  @Column(name="postal_code")
  private String postalCode;

  @Column(name="email_verified")
  private Boolean emailVerified; // boolean에서 Boolean로 변경

  @Column(name="created_at")
  private Long createdAt;

  @Column(name="updated_at")
  private Long updatedAt;

  @Column(name="last_login")
  private Long lastLogin;

  @Column(name="login_attempts")
  private Integer loginAttempts;

  @Column(name="account_locked")
  private Boolean accountLocked; // boolean에서 Boolean로 변경

  // Google OAuth 관련 필드
  @Column(name="oauth_provider")
  private String oauthProvider;

  @Column(name="oauth_id")
  private String oauthId;

  @Column(name="profile_picture_url")
  private String profilePictureUrl;

  // === 비즈니스 메서드들 ===

  /**
   * 이메일 인증 처리
   */
  public void verifyEmail() {
    this.emailVerified = true;
    this.updatedAt = java.time.Instant.now().getEpochSecond();
  }

  /**
   * Google OAuth 로그인 정보 업데이트
   */
  public void updateOAuthInfo(String provider, String oauthId, String profilePictureUrl) {
    this.oauthProvider = provider;
    this.oauthId = oauthId;
    this.profilePictureUrl = profilePictureUrl;
    this.emailVerified = true; // OAuth 로그인은 이메일이 이미 검증됨
    this.updatedAt = java.time.Instant.now().getEpochSecond();
    this.lastLogin = java.time.Instant.now().getEpochSecond();
  }

  /**
   * 이메일 인증 여부 확인 (boolean 타입 반환)
   */
  public boolean isEmailVerified() {
    return this.emailVerified != null && this.emailVerified;
  }

  /**
   * 계정 잠금 여부 확인 (boolean 타입 반환)
   */
  public boolean isAccountLocked() {
    return this.accountLocked != null && this.accountLocked;
  }

  /**
   * getEmailVerified() 메서드 명시적 추가 (호환성을 위해)
   */
  public Boolean getEmailVerified() {
    return this.emailVerified;
  }

  /**
   * getAccountLocked() 메서드 명시적 추가 (호환성을 위해)
   */
  public Boolean getAccountLocked() {
    return this.accountLocked;
  }

  public void patchUserFromDto(UserUpdateRequestDTO userUpdateRequestDTO, PasswordEncoder passwordEncoder) {
    if (userUpdateRequestDTO.getFirstName() != null && !userUpdateRequestDTO.getFirstName().trim().isEmpty())
      this.firstName = userUpdateRequestDTO.getFirstName().trim();

    if (userUpdateRequestDTO.getLastName() != null && !userUpdateRequestDTO.getLastName().trim().isEmpty())
      this.lastName = userUpdateRequestDTO.getLastName().trim();

    if (userUpdateRequestDTO.getEmail() != null && !userUpdateRequestDTO.getEmail().trim().isEmpty())
      this.email = userUpdateRequestDTO.getEmail().trim();

    if (userUpdateRequestDTO.getPassword() != null && !userUpdateRequestDTO.getPassword().trim().isEmpty())
      this.password = passwordEncoder.encode(userUpdateRequestDTO.getPassword());

    if (userUpdateRequestDTO.getBirthDate() != null)
      this.birthDate = userUpdateRequestDTO.getBirthDate();

    if (userUpdateRequestDTO.getCountry() != null)
      this.country = userUpdateRequestDTO.getCountry().trim();

    if (userUpdateRequestDTO.getPostalCode() != null)
      this.postalCode = userUpdateRequestDTO.getPostalCode().trim();

    this.updatedAt = java.time.Instant.now().getEpochSecond();
  }

  public void patchOAuthUserFromDto(OAuthUserUpdateRequestDTO oAuthUserUpdateRequestDTO, PasswordEncoder passwordEncoder) {
    if (oAuthUserUpdateRequestDTO.getFirstName() != null && !oAuthUserUpdateRequestDTO.getFirstName().trim().isEmpty())
      this.firstName = oAuthUserUpdateRequestDTO.getFirstName().trim();

    if (oAuthUserUpdateRequestDTO.getLastName() != null && !oAuthUserUpdateRequestDTO.getLastName().trim().isEmpty())
      this.lastName = oAuthUserUpdateRequestDTO.getLastName().trim();

    if (oAuthUserUpdateRequestDTO.getBirthDate() != null)
      this.birthDate = oAuthUserUpdateRequestDTO.getBirthDate();

    if (oAuthUserUpdateRequestDTO.getCountry() != null)
      this.country = oAuthUserUpdateRequestDTO.getCountry().trim();

    if (oAuthUserUpdateRequestDTO.getPostalCode() != null)
      this.postalCode = oAuthUserUpdateRequestDTO.getPostalCode().trim();

    this.updatedAt = java.time.Instant.now().getEpochSecond();
  }

  /**
   * 비밀번호 업데이트
   */
  public void updatePassword(String encodedPassword) {
    this.password = encodedPassword;
    this.updatedAt = java.time.Instant.now().getEpochSecond();
  }
}