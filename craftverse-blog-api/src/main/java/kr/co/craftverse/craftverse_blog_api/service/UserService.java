package kr.co.craftverse.craftverse_blog_api.service;

import jakarta.transaction.Transactional;
import java.time.Instant;
import kr.co.craftverse.craftverse_blog_api.exception.DuplicateResourceException;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserRegistrationRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.UserResponseDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

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
        .birthDate(userRegistrationRequestDTO.getBirthDate()) // Assuming birthDate is already in UTC timestamp
        .country(userRegistrationRequestDTO.getCountry())
        .postalCode(userRegistrationRequestDTO.getPostalCode())
        .emailVerified(false)
        .createdAt(currentTime)
        .updatedAt(currentTime)
        .loginAttempts(0)
        .accountLocked(false)
        .build();

    User savedUser = userRepository.save(user);

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
}