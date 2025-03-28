package kr.co.craftverse.craftverse_blog_api.service;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import kr.co.craftverse.craftverse_blog_api.common.exception.EmptyDataException;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.Article;
import kr.co.craftverse.craftverse_blog_api.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {
  private final ArticleRepository articleRepository;

  public List<ArticleDTO> getAll() {
    List<Article> articles = articleRepository.findAll();

    if (articles.isEmpty())
      throw new EmptyDataException("articles array is null.");

    List<ArticleDTO> articleDTOs = articles.stream()
        .map(article -> ArticleDTO.builder()
            .id(article.getId())
            .title(article.getTitle())
            .content(article.getContent())
            .category(article.getCategory())
            .language(article.getLanguage())
            .isPremium(article.getIsPremium())
            .createdAt(article.getCreatedAt())
            .updatedAt(article.getUpdatedAt())
            .viewsCount(article.getViewsCount())
            .slug(article.getSlug())
            .metaDescription(article.getMetaDescription())
            .build())
        .collect(Collectors.toList());

    return articleDTOs;
  }

  public ArticleDTO getById(long id) {
    Article article = articleRepository.getById(id);
    ArticleDTO articleDTO = ArticleDTO.builder()
        .id(article.getId())
        .title(article.getTitle())
        .content(article.getContent())
        .category(article.getCategory())
        .language(article.getLanguage())
        .isPremium(article.getIsPremium())
        .createdAt(article.getCreatedAt())
        .updatedAt(article.getUpdatedAt())
        .viewsCount(article.getViewsCount())
        .slug(article.getSlug())
        .metaDescription(article.getMetaDescription())
        .build();
    return articleDTO;
  }

  public Article create(ArticleDTO articleDTO) {
    Article article = Article.builder()
        .title(articleDTO.getTitle())
        .content(articleDTO.getContent())
        .category(articleDTO.getCategory())
        .language(articleDTO.getLanguage())
        .isPremium(articleDTO.getIsPremium())
        .createdAt(articleDTO.getCreatedAt())
        .updatedAt(articleDTO.getUpdatedAt())
        .viewsCount(articleDTO.getViewsCount())
        .slug(articleDTO.getSlug())
        .metaDescription(articleDTO.getMetaDescription())
        .build();
    articleRepository.save(article);
    return article;
  }

  public ArticleDTO update(long id, ArticleDTO articleDTO) throws IllegalAccessException {
    Article article = articleRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Article not found."));
    try {
      articleRepository.save(articleDTO.patchArticle(article));
      return articleDTO;
    } catch (Exception e) {
      throw new IllegalAccessException("didn't save");
    }
  }

  public void delete(long id) throws Exception {
    Article article = articleRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Article not found"));
    articleRepository.delete(article);
  }
}