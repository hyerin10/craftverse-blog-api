package kr.co.craftverse.craftverse_blog_api.config;

import kr.co.craftverse.craftverse_blog_api.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

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
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(Customizer.withDefaults())
        .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        )
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/payments/webhook", "/article/purchases", "/article/purchase", "/sitemap.xml", "/auth/csrf", "/auth/user", "/auth/user/oauth", "/auth/login", "/auth/logout", "/auth/me", "/auth/verify-email", "/auth/resend-verification", "/auth/google/url","/auth/google/callback", "/auth/google/callback/**", "/oauth2/**", "/auth/google/login", "/auth/refresh").permitAll()
            .requestMatchers("/auth/password-reset/reset", "/auth/password-reset/verify-code", "/auth/password-reset/send-code").permitAll()
            .requestMatchers("/article/*/views").permitAll()
            .requestMatchers("/article/**").permitAll()
            .requestMatchers("/articles").permitAll()
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
