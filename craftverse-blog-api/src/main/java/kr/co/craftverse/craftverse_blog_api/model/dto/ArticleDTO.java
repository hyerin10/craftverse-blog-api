package kr.co.craftverse.craftverse_blog_api.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArticleDTO {
  @NotNull
  private Long id;
  @NotNull
  private String title;
  @NotNull
  private String content;
  private String category;
  private String language;
  private Boolean isPremium;
  private Long createdAt;
  private Long updatedAt;
  private Integer viewsCount;
  private String slug;
  private String metaDescription;
}