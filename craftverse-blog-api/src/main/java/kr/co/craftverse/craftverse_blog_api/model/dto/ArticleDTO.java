package kr.co.craftverse.craftverse_blog_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArticleDTO {
  private Long id;
  private String title;
  private String content;
  private String category;
  private String language;
  private Boolean isPremium;
  private Long createdAt;
  private Long updatedAt;
  private Integer viewCount;
  private String slug;
  private String metaDescription;
  private Boolean isFullContentAvailable;

  // 프리미엄 가격 필드 추가
  private BigDecimal premiumPrice;
  private Integer expectationCount;

  // 프론트엔드용 추가 필드들
  private Boolean hasPremiumAccess; // 현재 사용자가 프리미엄 접근 권한을 가지고 있는지
  private Boolean isContentFiltered; // 콘텐츠가 필터링되었는지 (30%만 제공)
}