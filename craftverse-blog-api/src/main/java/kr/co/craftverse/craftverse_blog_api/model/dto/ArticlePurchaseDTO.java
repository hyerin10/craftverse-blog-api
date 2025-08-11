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
}