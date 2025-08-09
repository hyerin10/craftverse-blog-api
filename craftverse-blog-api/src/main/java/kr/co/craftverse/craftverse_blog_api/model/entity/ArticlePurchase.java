package kr.co.craftverse.craftverse_blog_api.model.entity;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "article_purchases")
public class ArticlePurchase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "article_id_ko")
  private Long articleIdKo;

  @Column(name = "article_id_en")
  private Long articleIdEn;

  @Column(name = "purchase_date", nullable = false)
  private Long purchaseDate; // UTC timestamp (milliseconds)

  @Column(name = "purchase_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal purchasePrice;

  @Column(name = "payment_status", nullable = false, length = 20)
  private String paymentStatus; // 'pending', 'completed', 'failed', 'cancelled'

  // 결제 관련 필드 추가
  @Column(name = "payment_key", nullable = false, length = 255)
  private String paymentKey;

  @Column(name = "order_id", nullable = false, length = 255)
  private String orderId;

  @Column(name = "payment_method", length = 50)
  private String paymentMethod; // 'card', 'virtual_account', etc.

  // 승인/완료 관련 필드
  @Column(name = "approved_at")
  private Long approvedAt; // UTC timestamp (milliseconds)

  @Column(name = "checkout_url", length = 500)
  private String checkoutUrl;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Long createdAt; // UTC timestamp (milliseconds)

  @Column(name = "updated_at", nullable = false)
  private Long updatedAt; // UTC timestamp (milliseconds)

  // JPA 생명주기 콜백으로 타임스탬프 자동 설정
  @PrePersist
  protected void onCreate() {
    long now = System.currentTimeMillis();
    if (this.createdAt == null) {
      this.createdAt = now;
    }
    this.updatedAt = now;
    if (this.purchaseDate == null) {
      this.purchaseDate = now;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = System.currentTimeMillis();
  }

  // 유틸리티 메서드들
  public boolean isCompleted() {
    return PAYMENT_STATUS_COMPLETED.equals(this.paymentStatus);
  }

  public boolean isPending() {
    return PAYMENT_STATUS_PENDING.equals(this.paymentStatus);
  }

  public boolean isFailed() {
    return PAYMENT_STATUS_FAILED.equals(this.paymentStatus);
  }

  // UTC 타임스탬프를 LocalDateTime으로 변환하는 헬퍼 메서드
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

  // 언어별 아티클 ID 설정 메서드
  public void setArticleIdByLanguage(String language, Long articleId) {
    if (LANGUAGE_EN.equals(language)) {
      this.articleIdEn = articleId;
    } else {
      this.articleIdKo = articleId;
    }
  }
}