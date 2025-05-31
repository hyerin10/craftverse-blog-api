package kr.co.craftverse.craftverse_blog_api.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import kr.co.craftverse.craftverse_blog_api.model.entity.Article;
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
  @Pattern(regexp = "ko|en")
  private String language;
  private Boolean isPremium;
  private Long createdAt;
  private Long updatedAt;
  private Integer viewCount;
  private String slug;
  private String metaDescription;

  public Article patchArticle(Article article) {
    Long newId = this.id != null ? this.id : article.getId();
    String newTitle = this.title != null ? this.title : article.getTitle();
    String newContent = this.content != null ? this.content : article.getContent();
    String newCategory = this.category != null ? this.category : article.getCategory();
    String newLanguage = this.language != null ? this.language : article.getLanguage();
    Boolean newIsPremium = this.isPremium != null ? this.isPremium : article.getIsPremium();
    Long newCreatedAt = this.createdAt != null ? this.createdAt : article.getCreatedAt();
    Long newUpdatedAt = this.updatedAt != null ? this.updatedAt : article.getUpdatedAt();
    Integer newViewsCount = this.viewCount != null ? this.viewCount : article.getViewCount();
    String newSlug = this.slug != null ? this.slug : article.getSlug();
    String newMetaDescription = this.metaDescription != null ? this.metaDescription : article.getMetaDescription();

    Article updatedArticle = Article.builder()
        .id(newId)
        .title(newTitle)
        .content(newContent)
        .category(newCategory)
        .language(newLanguage)
        .isPremium(newIsPremium)
        .createdAt(newCreatedAt)
        .updatedAt(newUpdatedAt)
        .viewCount(newViewsCount)
        .slug(newSlug)
        .metaDescription(newMetaDescription)
        .build();
    return updatedArticle;
  }
}