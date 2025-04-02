package kr.co.craftverse.craftverse_blog_api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
  private long id;
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
  private boolean emailVerified;
  @Column(name="created_at")
  private Long createdAt;
  @Column(name="updated_at")
  private Long updatedAt;
  @Column(name="last_login")
  private Date lastLogin;
  @Column(name="login_attempts")
  private Integer loginAttempts;
  @Column(name="account_locked")
  private boolean accountLocked;
  // Google OAuth 관련 필드 추가
  @Column(name="oauth_provider")
  private String oauthProvider;
  @Column(name="oauth_id")
  private String oauthId;
  @Column(name="profile_picture_url")
  private String profilePictureUrl;

  public void verifyEmail() {
    this.emailVerified = true;
    this.updatedAt = java.time.Instant.now().getEpochSecond();
  }

  // Google OAuth 로그인 정보 업데이트 메서드
  public void updateOAuthInfo(String provider, String oauthId, String profilePictureUrl) {
    this.oauthProvider = provider;
    this.oauthId = oauthId;
    this.profilePictureUrl = profilePictureUrl;
    this.emailVerified = true; // OAuth 로그인은 이메일이 이미 검증됨
    this.updatedAt = java.time.Instant.now().getEpochSecond();
    this.lastLogin = new Date();
  }
}
