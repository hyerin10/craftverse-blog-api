package kr.co.craftverse.craftverse_blog_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ViewCountService {

  private final RedisTemplate<String, String> redisTemplate;

  private static final String VIEW_COUNT_KEY_PREFIX = "article:view:";

  /**
   * 방문자의 최근 방문 여부 확인
   * @param articleId 게시글 ID
   * @param visitorId 방문자 식별자
   * @return 최근에 방문했다면 true, 아니라면 false
   */
  public boolean hasRecentlyVisited(Long articleId, String visitorId) {
    String key = generateKey(articleId, visitorId);
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }

  /**
   * 방문 기록 저장
   * @param articleId 게시글 ID
   * @param visitorId 방문자 식별자
   * @param expirationTimeHours 만료 시간(시간)
   */
  public void recordVisit(Long articleId, String visitorId, int expirationTimeHours) {
    String key = generateKey(articleId, visitorId);
    redisTemplate.opsForValue().set(key, "1", expirationTimeHours, TimeUnit.HOURS);
  }

  /**
   * Redis 키 생성
   * @param articleId 게시글 ID
   * @param visitorId 방문자 식별자
   * @return 생성된 키
   */
  private String generateKey(Long articleId, String visitorId) {
    return VIEW_COUNT_KEY_PREFIX + articleId + ":" + visitorId;
  }
}