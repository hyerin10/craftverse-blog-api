package kr.co.craftverse.craftverse_blog_api.model.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PromotionResultDTO {
  private static final String WIN_SUCCESS = "WIN_SUCCESS";
  private static final String DUPLICATE_REJECTED = "DUPLICATE_REJECTED";
  private static final String OUT_OF_STOCK_MSG = "OUT_OF_STOCK";

  private static final String SUCCESS_STATUS = "SUCCESS";
  private static final String DUPLICATE_STATUS = "DUPLICATE";
  private static final String NO_STOCK_STATUS = "NO_STOCK";

  private final boolean success;
  private final String status;      // 비즈니스 상태 (WIN_SUCCESS, DUPLICATE_REJECTED, OUT_OF_STOCK)
  private final String errorCode;   // 에러 구분 (SUCCESS, DUPLICATE, NO_STOCK)
  private final Long remainStock;   // 남은 수량

  // 편의 메서드
  public boolean isDuplicate() {
    return DUPLICATE_STATUS.equals(this.errorCode);
  }

  public boolean isOutOfStock() {
    return NO_STOCK_STATUS.equals(this.errorCode);
  }

  public boolean isSuccess() {
    return SUCCESS_STATUS.equals(this.errorCode);
  }

  // 정적 팩토리 메서드
  public static PromotionResultDTO success(Long remainStock) {
    return new PromotionResultDTO(true, WIN_SUCCESS, SUCCESS_STATUS, remainStock);
  }

  public static PromotionResultDTO duplicate(Long currentStock) {
    return new PromotionResultDTO(false, DUPLICATE_REJECTED, DUPLICATE_STATUS, currentStock);
  }

  public static PromotionResultDTO outOfStock() {
    return new PromotionResultDTO(false, OUT_OF_STOCK_MSG, NO_STOCK_STATUS, 0L);
  }
}