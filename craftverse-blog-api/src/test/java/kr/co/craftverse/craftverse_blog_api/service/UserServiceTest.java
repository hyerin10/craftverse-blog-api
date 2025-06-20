package kr.co.craftverse.craftverse_blog_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.exception.DuplicateResourceException;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.model.dto.LoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserRegistrationRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserResponseDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import kr.co.craftverse.craftverse_blog_api.service.messaging.EmailProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private EmailProducer emailProducer;

  @Mock
  private Logger logger;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @InjectMocks
  private UserService userService;

  @Nested
  @DisplayName("회원가입 테스트")
  class RegisterUserTest {

    @Test
    @DisplayName("회원가입 성공")
    void registerUser_Success() {
      // given
      UserRegistrationRequestDTO requestDTO = createUserRegistrationRequestDTO();
      User savedUser = createUser();

      when(userRepository.existsByEmail(anyString())).thenReturn(false);
      when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
      when(userRepository.save(any(User.class))).thenReturn(savedUser);

      // when
      UserResponseDTO result = userService.registerUser(requestDTO);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getEmail()).isEqualTo("test@example.com");
      assertThat(result.getFirstName()).isEqualTo("John");
      assertThat(result.getLastName()).isEqualTo("Doe");

      verify(emailProducer, times(1)).sendVerificationEmail(anyString(), anyString());
      verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void registerUser_DuplicateEmail() {
      // given
      UserRegistrationRequestDTO requestDTO = createUserRegistrationRequestDTO();
      when(userRepository.existsByEmail(anyString())).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> userService.registerUser(requestDTO))
          .isInstanceOf(DuplicateResourceException.class)
          .hasMessage("Email already exists");
    }
  }

  @Nested
  @DisplayName("로그인 테스트")
  class LoginTest {

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
      // given
      LoginRequestDTO requestDTO = LoginRequestDTO.builder()
          .email("test@example.com")
          .password("password123")
          .build();

      User user = createUser();
      String accessToken = "jwt.access.token";

      when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
      when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
      when(jwtTokenProvider.createAccessToken(any(), anyString())).thenReturn(accessToken);

      // when
      String result = userService.login(requestDTO);

      // then
      assertThat(result).isEqualTo(accessToken);
      verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 실패 - 사용자 없음")
    void login_UserNotFound() {
      // given
      LoginRequestDTO requestDTO = LoginRequestDTO.builder()
          .email("notfound@example.com")
          .password("password123")
          .build();

      when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.login(requestDTO))
          .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_WrongPassword() {
      // given
      LoginRequestDTO requestDTO = LoginRequestDTO.builder()
          .email("test@example.com")
          .password("wrongpassword")
          .build();

      User user = createUser();

      when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
      when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

      // when & then
      assertThatThrownBy(() -> userService.login(requestDTO))
          .isInstanceOf(UnauthorizedException.class);
    }
  }

  // Helper methods
  private UserRegistrationRequestDTO createUserRegistrationRequestDTO() {
    return UserRegistrationRequestDTO.builder()
        .firstName("John")
        .lastName("Doe")
        .email("test@example.com")
        .password("password123")
        .birthDate(946684800L)
        .country("KR")
        .postalCode("12345")
        .build();
  }

  private User createUser() {
    return User.builder()
        .id(1L)
        .firstName("John")
        .lastName("Doe")
        .email("test@example.com")
        .password("encodedPassword")
        .birthDate(946684800L)
        .country("KR")
        .postalCode("12345")
        .emailVerified(false)
        .createdAt(System.currentTimeMillis() / 1000)
        .updatedAt(System.currentTimeMillis() / 1000)
        .loginAttempts(0)
        .accountLocked(false)
        .build();
  }
}