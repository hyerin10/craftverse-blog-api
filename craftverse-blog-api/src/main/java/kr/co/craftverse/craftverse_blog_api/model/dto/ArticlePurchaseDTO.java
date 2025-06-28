package kr.co.craftverse.craftverse_blog_api.model.dto;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.LANGUAGE_EN;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PAYMENT_STATUS_COMPLETED;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PAYMENT_STATUS_FAILED;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PAYMENT_STATUS_PENDING;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 아티클 구매 정보 DTO (UTC 타임스탬프 사용)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ArticlePurchaseDTO {

  private Long id;
  private Long userId;
  private Long articleIdKo;
  private Long articleIdEn;
  private Long purchaseDate; // UTC timestamp (milliseconds)
  private BigDecimal purchasePrice;
  private String paymentStatus;
  private String paymentKey;
  private String orderId;
  private String paymentMethod;
  private Long approvedAt; // UTC timestamp (milliseconds)
  private Long createdAt; // UTC timestamp (milliseconds)
  private Long updatedAt; // UTC timestamp (milliseconds)

  // 조인된 아티클 정보 (선택적)
  private String articleTitle;
  private String articleDescription;

  // UTC 타임스탬프를 LocalDateTime으로 변환하는 헬퍼 메서드들
  public LocalDateTime getPurchaseDateAsLocalDateTime() {
    return this.purchaseDate != null
        ? LocalDateTime.ofInstant(Instant.ofEpochMilli(this.purchaseDate), ZoneOffset.UTC)
        : null;
  }

  public LocalDateTime getApprovedAtAsLocalDateTime() {
    return this.approvedAt != null
        ? LocalDateTime.ofInstant(Instant.ofEpochMilli(this.approvedAt), ZoneOffset.UTC)
        : null;
  }

  public LocalDateTime getCreatedAtAsLocalDateTime() {
    return this.createdAt != null
        ? LocalDateTime.ofInstant(Instant.ofEpochMilli(this.createdAt), ZoneOffset.UTC)
        : null;
  }

  public LocalDateTime getUpdatedAtAsLocalDateTime() {
    return this.updatedAt != null
        ? LocalDateTime.ofInstant(Instant.ofEpochMilli(this.updatedAt), ZoneOffset.UTC)
        : null;
  }

  // 언어별 아티클 ID 조회 메서드
  public Long getArticleIdByLanguage(String language) {
    if (LANGUAGE_EN.equals(language)) {
      return this.articleIdEn;
    }
    return this.articleIdKo; // 기본값은 한국어
  }

  // 상태 확인 메서드들
  public boolean isCompleted() {
    return PAYMENT_STATUS_COMPLETED.equals(this.paymentStatus);
  }

  public boolean isPending() {
    return PAYMENT_STATUS_PENDING.equals(this.paymentStatus);
  }

  public boolean isFailed() {
    return PAYMENT_STATUS_FAILED.equals(this.paymentStatus);
  }
}