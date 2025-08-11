package kr.co.craftverse.craftverse_blog_api.repository;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.ACCESS_TOKEN_PREFIX;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.REFRESH_TOKEN_PREFIX;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TokenRepository {
  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * 액세스 토큰 저장
   * @param userId 사용자 ID
   * @param token 토큰 문자열
   * @param expirationTimeInSeconds 만료 시간(초)
   */
  public void saveAccessToken(Long userId, String token, long expirationTimeInSeconds) {
    String key = ACCESS_TOKEN_PREFIX + userId;
    redisTemplate.opsForValue().set(key, token, expirationTimeInSeconds, TimeUnit.SECONDS);
  }

  /**
   * 리프레시 토큰 저장
   * @param userId 사용자 ID
   * @param token 토큰 문자열
   * @param expirationTimeInSeconds 만료 시간(초)
   */
  public void saveRefreshToken(Long userId, String token, long expirationTimeInSeconds) {
    String key = REFRESH_TOKEN_PREFIX + userId;
    redisTemplate.opsForValue().set(key, token, expirationTimeInSeconds, TimeUnit.SECONDS);
  }

  /**
   * 액세스 토큰 조회
   * @param userId 사용자 ID
   * @return 토큰 문자열
   */
  public String getAccessToken(Long userId) {
    String key = ACCESS_TOKEN_PREFIX + userId;
    Object value = redisTemplate.opsForValue().get(key);
    return value != null ? value.toString() : null;
  }

  /**
   * 리프레시 토큰 조회
   * @param userId 사용자 ID
   * @return 토큰 문자열
   */
  public String getRefreshToken(Long userId) {
    String key = REFRESH_TOKEN_PREFIX + userId;
    Object value = redisTemplate.opsForValue().get(key);
    return value != null ? value.toString() : null;
  }

  /**
   * 액세스 토큰 개별 삭제 (RTR용)
   * @param userId 사용자 ID
   */
  public void deleteAccessToken(Long userId) {
    redisTemplate.delete(ACCESS_TOKEN_PREFIX + userId);
  }

  /**
   * 리프레시 토큰 개별 삭제 (RTR용)
   * @param userId 사용자 ID
   */
  public void deleteRefreshToken(Long userId) {
    redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
  }

  /**
   * 모든 토큰 삭제 (로그아웃 시 사용)
   * @param userId 사용자 ID
   */
  public void deleteTokens(Long userId) {
    redisTemplate.delete(ACCESS_TOKEN_PREFIX + userId);
    redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
  }

  /**
   * 모든 토큰 삭제 (별칭 메서드)
   * @param userId 사용자 ID
   */
  public void deleteAllTokens(Long userId) {
    deleteTokens(userId);
  }

  /**
   * 액세스 토큰의 남은 만료 시간 조회 (초 단위)
   * @param userId 사용자 ID
   * @return 만료까지 남은 시간(초)
   */
  public Long getAccessTokenExpiration(Long userId) {
    String key = ACCESS_TOKEN_PREFIX + userId;
    return redisTemplate.getExpire(key, TimeUnit.SECONDS);
  }
}