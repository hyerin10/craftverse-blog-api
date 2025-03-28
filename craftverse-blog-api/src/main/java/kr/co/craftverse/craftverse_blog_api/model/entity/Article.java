package kr.co.craftverse.craftverse_blog_api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
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
}