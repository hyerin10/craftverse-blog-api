package kr.co.craftverse.craftverse_blog_api.config;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.BLACKLIST_PREFIX;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class JwtTokenProvider {
  @Value("${jwt.secret-key}")
  private String secretKey;

  @Value("${jwt.access-token-expiration-ms}")
  private long accessTokenValidityInMilliseconds;

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  private Key key;

  @PostConstruct
  protected void init() {
    String encodedKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    key = Keys.hmacShaKeyFor(encodedKey.getBytes());
  }

  // Access 토큰 생성
  public String createAccessToken(Long userId, String email) {
    Claims claims = Jwts.claims().setSubject(email);
    claims.put("userId", userId);

    Date now = new Date();
    Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

    return Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  // JWT 토큰에서 사용자 ID 추출
  public Long getUserId(String token) {
    return parseClaims(token).get("userId", Long.class);
  }

  // JWT 토큰에서 이메일 추출
  public String getEmail(String token) {
    return parseClaims(token).getSubject();
  }

  // 토큰 유효성 검사 - Redis 블랙리스트 확인 추가
  public boolean validateToken(String token) {
    try {
      // 1. 블랙리스트 확인
      String blacklistKey = BLACKLIST_PREFIX + token;
      Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);
      if (isBlacklisted != null && isBlacklisted) {
        log.error("Blacklisted token");
        return false;
      }

      // 2. JWT 서명 및 만료일 검증
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return true;
    } catch (SecurityException | MalformedJwtException e) {
      log.error("Invalid JWT signature", e);
    } catch (ExpiredJwtException e) {
      log.error("Expired JWT token", e);
    } catch (UnsupportedJwtException e) {
      log.error("Unsupported JWT token", e);
    } catch (IllegalArgumentException e) {
      log.error("JWT claims string is empty", e);
    }
    return false;
  }

  // 로그아웃 - 토큰을 블랙리스트에 추가
  public void blacklistToken(String token) {
    try {
      // 토큰의 남은 유효 시간 계산
      Claims claims = parseClaims(token);
      Date expiration = claims.getExpiration();
      Date now = new Date();
      long ttlInMillis = expiration.getTime() - now.getTime();

      // 음수면 이미 만료된 토큰
      if (ttlInMillis > 0) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(blacklistKey, "1", ttlInMillis, TimeUnit.MILLISECONDS);
        log.info("Token added to blacklist until expiration");
      } else {
        log.info("Token already expired, no need to blacklist");
      }
    } catch (Exception e) {
      log.error("Error during token blacklisting", e);
    }
  }

  // JWT 토큰 파싱
  private Claims parseClaims(String token) {
    try {
      return Jwts.parserBuilder()
          .setSigningKey(key)
          .build()
          .parseClaimsJws(token)
          .getBody();
    } catch (ExpiredJwtException e) {
      return e.getClaims();
    }
  }

  // Request에서 JWT 토큰 추출
  public String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}