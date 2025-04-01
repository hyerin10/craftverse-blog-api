package kr.co.craftverse.craftverse_blog_api.service;

import jakarta.transaction.Transactional;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.exception.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.model.dto.LoginRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import kr.co.craftverse.craftverse_blog_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public String login(LoginRequestDTO loginRequestDTO) {
    // 이메일로 사용자 조회
    User user = userRepository.findByEmail(loginRequestDTO.getEmail())
        .orElseThrow(UnauthorizedException::new);

    // 비밀번호 확인
    if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword()))
      throw new UnauthorizedException();

    // JWT 토큰 생성
    String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());

    userRepository.save(user);

    return accessToken;
  }
}
