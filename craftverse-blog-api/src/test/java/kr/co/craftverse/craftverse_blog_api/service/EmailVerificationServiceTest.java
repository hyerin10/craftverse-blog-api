package kr.co.craftverse.craftverse_blog_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.model.dto.VerifyEmailDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService 테스트")
class EmailVerificationServiceTest {

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private UserRepository userRepository;

  @Mock
  private Logger logger;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @InjectMocks
  private EmailVerificationService emailVerificationService;

  @Test
  @DisplayName("이메일 인증 성공")
  void verifyEmail_Success() {
    // given
    VerifyEmailDTO requestDTO = VerifyEmailDTO.builder()
        .email("test@example.com")
        .code("123456")
        .build();

    User user = createUnverifiedUser();

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(anyString())).thenReturn("123456");
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

    // when
    boolean result = emailVerificationService.verifyEmail(requestDTO);

    // then
    assertThat(result).isTrue();
    verify(userRepository, times(1)).save(any(User.class));
    verify(redisTemplate, times(1)).delete(anyString());
  }

  @Test
  @DisplayName("이메일 인증 실패 - 잘못된 코드")
  void verifyEmail_InvalidCode() {
    // given
    VerifyEmailDTO requestDTO = VerifyEmailDTO.builder()
        .email("test@example.com")
        .code("000000")
        .build();

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(anyString())).thenReturn("123456");

    // when & then
    assertThatThrownBy(() -> emailVerificationService.verifyEmail(requestDTO))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  @DisplayName("이메일 인증 실패 - 이미 인증된 사용자")
  void verifyEmail_AlreadyVerified() {
    // given
    VerifyEmailDTO requestDTO = VerifyEmailDTO.builder()
        .email("test@example.com")
        .code("123456")
        .build();

    User verifiedUser = createVerifiedUser();

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(anyString())).thenReturn("123456");
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(verifiedUser));

    // when & then
    assertThatThrownBy(() -> emailVerificationService.verifyEmail(requestDTO))
        .isInstanceOf(UnauthorizedException.class);

    verify(redisTemplate, times(1)).delete(anyString());
  }

  private User createUnverifiedUser() {
    return User.builder()
        .id(1L)
        .email("test@example.com")
        .emailVerified(false)
        .build();
  }

  private User createVerifiedUser() {
    return User.builder()
        .id(1L)
        .email("test@example.com")
        .emailVerified(true)
        .build();
  }
}