package kr.co.craftverse.craftverse_blog_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
  private Boolean isFullContentAvailable; // 새로 추가된 필드
}
