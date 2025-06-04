package kr.co.craftverse.craftverse_blog_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.exception.DuplicateResourceException;
import kr.co.craftverse.craftverse_blog_api.model.dto.GoogleLoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.LoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserRegistrationRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserResponseDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.VerifyEmailDTO;
import kr.co.craftverse.craftverse_blog_api.service.EmailVerificationService;
import kr.co.craftverse.craftverse_blog_api.service.OAuth2Service;
import kr.co.craftverse.craftverse_blog_api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AuthController 테스트")
@Transactional
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private OAuth2Service oAuth2Service;

  @MockitoBean
  private EmailVerificationService emailVerificationService;

  @Nested
  @DisplayName("일반 회원가입/로그인 테스트")
  class RegularAuthTest {

    @Test
    @DisplayName("회원가입 성공")
    void registerUser_Success() throws Exception {
      // given
      UserRegistrationRequestDTO requestDTO = createUserRegistrationRequestDTO();
      UserResponseDTO responseDTO = createUserResponseDTO();

      when(userService.registerUser(any(UserRegistrationRequestDTO.class)))
          .thenReturn(responseDTO);

      // when & then
      mockMvc.perform(post("/auth/register")
              .with(csrf()) // CSRF 토큰 추가
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.user.id").value(1L))
          .andExpect(jsonPath("$.result.user.email").value("test@example.com"))
          .andExpect(jsonPath("$.result.user.firstName").value("John"))
          .andExpect(jsonPath("$.result.user.lastName").value("Doe"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void registerUser_DuplicateEmail() throws Exception {
      // given
      UserRegistrationRequestDTO requestDTO = createUserRegistrationRequestDTO();

      when(userService.registerUser(any(UserRegistrationRequestDTO.class)))
          .thenThrow(new DuplicateResourceException("Email already exists"));

      // when & then
      mockMvc.perform(post("/auth/register")
              .with(csrf()) // CSRF 토큰 추가
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 실패 - 유효하지 않은 입력")
    void registerUser_InvalidInput() throws Exception {
      // given
      UserRegistrationRequestDTO requestDTO = UserRegistrationRequestDTO.builder()
          .email("invalid-email") // 잘못된 이메일 형식
          .firstName("") // 빈 문자열
          .lastName("") // 빈 문자열
          .password("123") // 패스워드 정책 위반 (너무 짧고, 대문자/특수문자 없음)
          .build();

      // when & then
      mockMvc.perform(post("/auth/register")
              .with(csrf()) // CSRF 토큰 추가
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() throws Exception {
      // given
      LoginRequestDTO requestDTO = LoginRequestDTO.builder()
          .email("test@example.com")
          .password("Password123!")
          .build();

      String accessToken = "jwt.access.token";
      when(userService.login(any(LoginRequestDTO.class)))
          .thenReturn(accessToken);

      // when & then
      mockMvc.perform(post("/auth/login")
              .with(csrf()) // CSRF 토큰 추가
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.accessToken").value(accessToken));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 자격증명")
    void login_InvalidCredentials() throws Exception {
      // given
      LoginRequestDTO requestDTO = LoginRequestDTO.builder()
          .email("test@example.com")
          .password("wrongpassword")
          .build();

      when(userService.login(any(LoginRequestDTO.class)))
          .thenThrow(new UnauthorizedException());

      // when & then
      mockMvc.perform(post("/auth/login")
              .with(csrf()) // CSRF 토큰 추가
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("이메일 인증 성공")
    void verifyEmail_Success() throws Exception {
      // given
      VerifyEmailDTO requestDTO = VerifyEmailDTO.builder()
          .email("test@example.com")
          .code("123456")
          .build();

      when(emailVerificationService.verifyEmail(any(VerifyEmailDTO.class)))
          .thenReturn(true);

      // when & then
      mockMvc.perform(post("/auth/verify-email")
              .with(csrf()) // CSRF 토큰 추가
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("이메일 인증 실패 - 잘못된 인증 코드")
    void verifyEmail_InvalidCode() throws Exception {
      // given
      VerifyEmailDTO requestDTO = VerifyEmailDTO.builder()
          .email("test@example.com")
          .code("000000")
          .build();

      when(emailVerificationService.verifyEmail(any(VerifyEmailDTO.class)))
          .thenThrow(new UnauthorizedException());

      // when & then
      mockMvc.perform(post("/auth/verify-email")
              .with(csrf()) // CSRF 토큰 추가
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("구글 소셜 로그인 테스트")
  class GoogleOAuthTest {

    @Test
    @DisplayName("구글 인증 URL 생성 - 로그인")
    void getGoogleAuthUrl_Login() throws Exception {
      // given
      String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=...";
      when(oAuth2Service.getGoogleAuthUrl("login"))
          .thenReturn(authUrl);

      // when & then
      mockMvc.perform(get("/auth/google/url")
              .param("action", "login"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.authUrl").value(authUrl))
          .andExpect(jsonPath("$.result.action").value("login"));
    }

    @Test
    @DisplayName("구글 인증 URL 생성 - 회원가입")
    void getGoogleAuthUrl_Signup() throws Exception {
      // given
      String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=...";
      when(oAuth2Service.getGoogleAuthUrl("signup"))
          .thenReturn(authUrl);

      // when & then
      mockMvc.perform(get("/auth/google/url")
              .param("action", "signup"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.authUrl").value(authUrl))
          .andExpect(jsonPath("$.result.action").value("signup"));
    }

    @Test
    @DisplayName("구글 인증 URL 생성 - 기본값(로그인)")
    void getGoogleAuthUrl_Default() throws Exception {
      // given
      String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=...";
      when(oAuth2Service.getGoogleAuthUrl("login"))
          .thenReturn(authUrl);

      // when & then
      mockMvc.perform(get("/auth/google/url"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.authUrl").value(authUrl))
          .andExpect(jsonPath("$.result.action").value("login"));
    }

    @Test
    @DisplayName("구글 인증 URL 생성 - 잘못된 action 파라미터")
    void getGoogleAuthUrl_InvalidAction() throws Exception {
      // given
      String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=...";
      when(oAuth2Service.getGoogleAuthUrl("login"))
          .thenReturn(authUrl);

      // when & then
      mockMvc.perform(get("/auth/google/url")
              .param("action", "invalid"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.action").value("login")); // 기본값으로 설정됨
    }

    @Test
    @DisplayName("구글 로그인 API 성공")
    void googleLogin_Success() throws Exception {
      // given
      GoogleLoginRequestDTO requestDTO = GoogleLoginRequestDTO.builder()
          .code("google_auth_code")
          .build();

      Map<String, String> tokenInfo = new HashMap<>();
      tokenInfo.put("accessToken", "jwt.access.token");
      tokenInfo.put("user", "{\"id\":1,\"email\":\"test@gmail.com\"}");

      when(oAuth2Service.loginWithGoogle(anyString()))
          .thenReturn(tokenInfo);

      // when & then
      mockMvc.perform(post("/auth/google/login")
              .with(csrf()) // CSRF 토큰 추가
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.accessToken").value("jwt.access.token"))
          .andExpect(jsonPath("$.result.user").value("{\"id\":1,\"email\":\"test@gmail.com\"}"));
    }

    @Test
    @DisplayName("만료된 인증 코드로 로그인 실패")
    void googleLogin_ExpiredCode() throws Exception {
      // given
      GoogleLoginRequestDTO requestDTO = GoogleLoginRequestDTO.builder()
          .code("expired_authorization_code")
          .build();

      when(oAuth2Service.loginWithGoogle("expired_authorization_code"))
          .thenThrow(new UnauthorizedException());

      // when & then
      mockMvc.perform(post("/auth/google/login")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("이미 사용된 인증 코드로 로그인 실패")
    void googleLogin_CodeAlreadyUsed() throws Exception {
      // given
      GoogleLoginRequestDTO requestDTO = GoogleLoginRequestDTO.builder()
          .code("already_used_code")
          .build();

      when(oAuth2Service.loginWithGoogle("already_used_code"))
          .thenThrow(new UnauthorizedException());

      // when & then
      mockMvc.perform(post("/auth/google/login")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("사용자가 구글 로그인 권한을 거부한 경우")
    void googleLogin_UserDenied() throws Exception {
      // given
      GoogleLoginRequestDTO requestDTO = GoogleLoginRequestDTO.builder()
          .error("access_denied")
          .build();

      // when & then
      mockMvc.perform(post("/auth/google/login")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("요청 파라미터 누락")
    void googleLogin_MissingParameters() throws Exception {
      // given
      GoogleLoginRequestDTO requestDTO = GoogleLoginRequestDTO.builder()
          .build(); // code가 null

      // when & then
      mockMvc.perform(post("/auth/google/login")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("빈 문자열 코드")
    void googleLogin_EmptyCode() throws Exception {
      // given
      GoogleLoginRequestDTO requestDTO = GoogleLoginRequestDTO.builder()
          .code("   ") // 공백 문자열
          .build();

      // when & then
      mockMvc.perform(post("/auth/google/login")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDTO)))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("공통 인증 기능 테스트")
  class CommonAuthTest {

    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshToken_Success() throws Exception {
      // given
      String newAccessToken = "new.jwt.access.token";
      when(oAuth2Service.refreshAccessToken(anyLong(), anyString(), anyString()))
          .thenReturn(newAccessToken);

      // when & then
      mockMvc.perform(post("/auth/refresh")
              .with(csrf()) // CSRF 토큰 추가
              .param("userId", "1")
              .param("email", "test@example.com")
              .param("refreshToken", "refresh.token"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.accessToken").value(newAccessToken));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰")
    void refreshToken_InvalidRefreshToken() throws Exception {
      // given
      when(oAuth2Service.refreshAccessToken(anyLong(), anyString(), anyString()))
          .thenThrow(new UnauthorizedException());

      // when & then
      mockMvc.perform(post("/auth/refresh")
              .with(csrf()) // CSRF 토큰 추가
              .param("userId", "1")
              .param("email", "test@example.com")
              .param("refreshToken", "invalid.refresh.token"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() throws Exception {
      // given
      doNothing().when(userService).logout(any());
      doNothing().when(oAuth2Service).logout(any());

      // when & then
      mockMvc.perform(post("/auth/logout")
              .with(csrf()) // CSRF 토큰 추가
              .header("Authorization", "Bearer jwt.access.token"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.message").value("로그아웃되었습니다."));
    }

    @Test
    @DisplayName("로그아웃 - 토큰 없음")
    void logout_NoToken() throws Exception {
      // given
      doNothing().when(userService).logout(any());
      doNothing().when(oAuth2Service).logout(any());

      // when & then
      mockMvc.perform(post("/auth/logout")
              .with(csrf())) // CSRF 토큰 추가
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.message").value("로그아웃되었습니다."));
    }
  }

  // Helper methods
  private UserRegistrationRequestDTO createUserRegistrationRequestDTO() {
    return UserRegistrationRequestDTO.builder()
        .firstName("John")
        .lastName("Doe")
        .email("test@example.com")
        .password("Password123!") // 대문자, 소문자, 숫자, 특수문자 포함
        .birthDate(946684800L) // 2000-01-01
        .country("KR")
        .postalCode("12345")
        .build();
  }

  private UserResponseDTO createUserResponseDTO() {
    return UserResponseDTO.builder()
        .id(1L)
        .firstName("John")
        .lastName("Doe")
        .email("test@example.com")
        .birthDate(946684800L)
        .country("KR")
        .postalCode("12345")
        .createdAt(System.currentTimeMillis() / 1000)
        .build();
  }
}