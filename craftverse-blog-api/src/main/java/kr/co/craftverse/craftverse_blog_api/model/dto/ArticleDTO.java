package kr.co.craftverse.craftverse_blog_api.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
  @Pattern(regexp = "overoll|tech|series")
  private String category;
  @Pattern(regexp = "ko|en|es")
  private String language;
  private Boolean isPremium;
  private Long createdAt;
  private Long updatedAt;
  private Integer viewsCount;
  private String slug;
  private String metaDescription;
}