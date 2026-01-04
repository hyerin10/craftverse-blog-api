package kr.co.craftverse.craftverse_blog_api.service;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.REDIS_PROMOTION_PREFIX;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.REDIS_PROMOTION_STOCK_PREFIX;

import java.util.Arrays;
import java.util.List;
import kr.co.craftverse.craftverse_blog_api.exception.OutOfStockException;
import kr.co.craftverse.craftverse_blog_api.model.dto.PromotionResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final DefaultRedisScript<List> promotionScript;

  public PromotionResultDTO tryParticipatePromotion(String userIp) {
    String userKey = REDIS_PROMOTION_PREFIX + userIp;

    try {
      // Lua 스크립트 실행
      List<?> results = redisTemplate.execute(
          promotionScript,
          Arrays.asList(userKey, REDIS_PROMOTION_STOCK_PREFIX)
      );

      if (results.isEmpty()) {
        log.error("[SCRIPT_ERROR] Lua script returned empty result");
        throw new OutOfStockException("준비된 수량이 모두 소진되었습니다.");
      }

      Long resultCode = convertToLong(results.get(0));
      Long remainStock = convertToLong(results.get(1));

      log.debug("[PROMOTION_RESULT] IP: {} | ResultCode: {} | RemainStock: {}",
          userIp, resultCode, remainStock);

      if (resultCode == 1L) {
        log.info("[PROMOTION_SUCCESS] IP: {} | 남은 재고: {}", userIp, remainStock);
        return PromotionResultDTO.success(remainStock);
      } else if (resultCode == -1L) {
        log.warn("[PROMOTION_DUPLICATE] IP: {}", userIp);
        return PromotionResultDTO.duplicate(remainStock);
      } else {
        log.warn("[PROMOTION_OUT_OF_STOCK] IP: {}", userIp);
        return PromotionResultDTO.outOfStock();
      }
    } catch (Exception e) {
      log.error("[PROMOTION_ERROR] IP: {} | Error: {}", userIp, e.getMessage(), e);
      throw new OutOfStockException("프로모션 처리 중 오류가 발생했습니다.");
    }
  }

  /**
   * Redis에서 반환된 Number 객체를 안전하게 Long으로 변환
   */
  private Long convertToLong(Object value) {
    if (value == null)
      return 0L;
    if (value instanceof Long)
      return (Long) value;
    if (value instanceof Integer)
      return ((Integer) value).longValue();
    if (value instanceof String)
      return Long.parseLong((String) value);
    log.warn("[TYPE_CONVERSION_WARNING] Unexpected type: {}", value.getClass());
    return 0L;
  }
}