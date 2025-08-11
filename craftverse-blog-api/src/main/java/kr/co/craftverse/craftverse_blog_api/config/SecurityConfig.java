package kr.co.craftverse.craftverse_blog_api.config;

import java.util.Arrays;
import kr.co.craftverse.craftverse_blog_api.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtTokenProvider jwtTokenProvider;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 허용할 오리진 설정
    configuration.setAllowedOrigins(Arrays.asList("https://craftverse.co.kr"));

    // 허용할 HTTP 메서드 설정 (OPTIONS 포함 필수)
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

    // 허용할 헤더 설정 - CSRF 토큰 헤더들 추가
    configuration.setAllowedHeaders(Arrays.asList(
        "*",
        "X-XSRF-TOKEN",
        "X-CSRF-TOKEN",
        "Authorization",
        "Content-Type",
        "Accept"
    ));

    // 인증 정보(쿠키, Authorization 헤더 등) 허용
    configuration.setAllowCredentials(true);

    // preflight 요청 캐시 시간 설정
    configuration.setMaxAge(3600L);

    // 노출할 헤더 설정 - CSRF 토큰 헤더 포함
    configuration.setExposedHeaders(Arrays.asList(
        "Authorization",
        "X-Total-Count",
        "X-XSRF-TOKEN",
        "X-CSRF-TOKEN"
    ));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // CORS 설정을 맨 앞에 배치
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf
            // 쿠키 기반 CSRF 토큰 저장소 사용 (SPA에 적합)
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            // CSRF 토큰을 request handler로 처리
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
        )
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/payments/**").permitAll()
            .requestMatchers("/auth/**").permitAll()
            .requestMatchers("/article/**").permitAll()
            .requestMatchers("/articles/**").permitAll()
            .requestMatchers("/sitemap.xml").permitAll()
            .requestMatchers("/", "/home", "/about").permitAll()
            .anyRequest().authenticated()
        )
        .sessionManagement(sessionManagement ->
            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}