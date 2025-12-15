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

  @Column(name="expectation_count")
  private Integer expectationCount;

  public void incrementViewCount() {
    if (this.viewCount == null)
      this.viewCount = 1;
    else
      this.viewCount += 1;
  }

  public void incrementExpectationCount() {
    if (this.expectationCount == null)
      this.expectationCount = 1;
    else
      this.expectationCount += 1;
  }
}