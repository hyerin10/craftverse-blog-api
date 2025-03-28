package kr.co.craftverse.craftverse_blog_api.service;

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
}