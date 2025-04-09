package kr.co.craftverse.craftverse_blog_api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name="article")
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
  @Column(name="views_count")
  private Integer viewsCount;
  @Column(name="created_at")
  private Long createdAt;
  @Column(name="updated_at")
  private Long updatedAt;
  @Column(name="slug")
  private String slug;
  @Column(name="meta_description")
  private String metaDescription;

  public void saveArticle(long id, ArticleDTO articleDTO) {
    this.id = id;
    this.title = articleDTO.getTitle();
    this.content = articleDTO.getContent();
    this.category = articleDTO.getCategory();
    this.isPremium = articleDTO.getIsPremium();
    this.language = articleDTO.getLanguage();
    this.viewsCount = articleDTO.getViewsCount();
    this.createdAt = articleDTO.getCreatedAt();
    this.updatedAt = articleDTO.getUpdatedAt();
    this.slug = articleDTO.getSlug();
    this.metaDescription = articleDTO.getMetaDescription();
  }

  public void updateModifiedTime(Long currentTimeMillis) {
    this.updatedAt = currentTimeMillis;
  }

  public Integer incrementViewsCount() {
    if (this.viewsCount == null)
      this.viewsCount = 1;
    else
      this.viewsCount = this.viewsCount + 1;
    return this.viewsCount;
  }
}