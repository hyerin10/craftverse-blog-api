package kr.co.craftverse.craftverse_blog_api.service;

import jakarta.transaction.Transactional;
import java.time.Instant;
import kr.co.craftverse.craftverse_blog_api.exception.DuplicateResourceException;
import kr.co.craftverse.craftverse_blog_api.exception.ResourceNotFoundException;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserRegistrationRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserResponseDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import kr.co.craftverse.craftverse_blog_api.service.messaging.EmailProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailProducer emailProducer;
  private final Logger logger;

  @Transactional
  public UserResponseDTO registerUser(UserRegistrationRequestDTO userRegistrationRequestDTO) {
    if (userRepository.existsByEmail(userRegistrationRequestDTO.getEmail()))
      throw new DuplicateResourceException("Email already exists");

    Long currentTime = Instant.now().getEpochSecond();

    User user = User.builder()
        .firstName(userRegistrationRequestDTO.getFirstName())
        .lastName(userRegistrationRequestDTO.getLastName())
        .email(userRegistrationRequestDTO.getEmail())
        .password(passwordEncoder.encode(userRegistrationRequestDTO.getPassword()))
        .birthDate(userRegistrationRequestDTO.getBirthDate())
        .country(userRegistrationRequestDTO.getCountry())
        .postalCode(userRegistrationRequestDTO.getPostalCode())
        .emailVerified(false)
        .createdAt(currentTime)
        .updatedAt(currentTime)
        .loginAttempts(0)
        .accountLocked(false)
        .build();

    User savedUser = userRepository.save(user);

    // 이메일 인증 코드 발송 요청
    emailProducer.sendVerificationEmail(savedUser.getEmail());

    logger.info("[UserService] 회원가입 완료 및 인증 이메일 발송 요청: {}", savedUser.getEmail());

    return UserResponseDTO.builder()
        .id(savedUser.getId())
        .firstName(savedUser.getFirstName())
        .lastName(savedUser.getLastName())
        .email(savedUser.getEmail())
        .birthDate(savedUser.getBirthDate())
        .country(savedUser.getCountry())
        .postalCode(savedUser.getPostalCode())
        .createdAt(savedUser.getCreatedAt())
        .build();
  }

  @Transactional
  public void resendVerificationEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

    if (user.isEmailVerified()) {
      logger.info("[UserService] 이미 인증된 이메일입니다: {}", email);
      return;
    }

    // 이메일 인증 코드 재발송
    emailProducer.sendVerificationEmail(email);
    logger.info("[UserService] 인증 이메일 재발송: {}", email);
  }

  public User findById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));
  }
}