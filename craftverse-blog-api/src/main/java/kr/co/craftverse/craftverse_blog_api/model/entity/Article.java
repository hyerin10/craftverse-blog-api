package kr.co.craftverse.craftverse_blog_api.model.entity;

import jakarta.persistence.*;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name="articles")
public class Article {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name="title")
  private String title;

  @Column(name="content")
  private String content;

  @Column(name="category")
  private String category;

  @Column(name="is_premium")
  private Boolean isPremium;

  @Column(name="language")
  private String language;

  @Column(name="view_count")
  private Integer viewCount;

  @Column(name="created_at")
  private Long createdAt;

  @Column(name="updated_at")
  private Long updatedAt;

  @Column(name="slug")
  private String slug;

  @Column(name="meta_description")
  private String metaDescription;

  // 프리미엄 가격 필드 추가
  @Column(name="premium_price", precision = 10, scale = 2)
  private BigDecimal premiumPrice;

  public void saveArticle(long id, ArticleDTO articleDTO) {
    this.id = id;
    this.title = articleDTO.getTitle();
    this.content = articleDTO.getContent();
    this.category = articleDTO.getCategory();
    this.isPremium = articleDTO.getIsPremium();
    this.language = articleDTO.getLanguage();
    this.viewCount = articleDTO.getViewCount();
    this.createdAt = articleDTO.getCreatedAt();
    this.updatedAt = articleDTO.getUpdatedAt();
    this.slug = articleDTO.getSlug();
    this.metaDescription = articleDTO.getMetaDescription();
    this.premiumPrice = articleDTO.getPremiumPrice();
  }

  public void incrementViewCount() {
    if (this.viewCount == null)
      this.viewCount = 1;
    else
      this.viewCount += 1;
  }

  // 프리미엄 가격 관련 유틸리티 메서드
  public boolean hasPremiumPrice() {
    return this.premiumPrice != null && this.premiumPrice.compareTo(BigDecimal.ZERO) > 0;
  }

  // 가격을 Long(원 단위)으로 반환하는 메서드 (결제 API용)
  public Long getPremiumPriceAsLong() {
    return this.premiumPrice != null ? this.premiumPrice.longValue() : null;
  }
}