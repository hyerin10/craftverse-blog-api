package kr.co.craftverse.craftverse_blog_api.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.NotFoundException;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticlePurchasesDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.Article;
import kr.co.craftverse.craftverse_blog_api.model.entity.ArticlePurchases;
import kr.co.craftverse.craftverse_blog_api.repository.ArticlePurchaseRepository;
import kr.co.craftverse.craftverse_blog_api.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {
  private final ArticleRepository articleRepository;
  private final ArticlePurchaseRepository articlePurchaseRepository;

  public List<ArticleDTO> getByLanguage(String language) {
    List<Article> articles = articleRepository.findByLanguage(language);

    if (articles.isEmpty())
      throw new NotFoundException("articles array is null.");

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
            .viewCount(article.getViewCount())
            .slug(article.getSlug())
            .metaDescription(article.getMetaDescription())
            .build())
        .collect(Collectors.toList());

    return articleDTOs;
  }

  public ArticleDTO getById(long id) {
    Article article = articleRepository.findById(id).orElseThrow(() -> new NotFoundException("not found."));;
    ArticleDTO articleDTO = ArticleDTO.builder()
        .id(article.getId())
        .title(article.getTitle())
        .content(article.getContent())
        .category(article.getCategory())
        .language(article.getLanguage())
        .isPremium(article.getIsPremium())
        .createdAt(article.getCreatedAt())
        .updatedAt(article.getUpdatedAt())
        .viewCount(article.getViewCount())
        .slug(article.getSlug())
        .metaDescription(article.getMetaDescription())
        .build();
    return articleDTO;
  }

  public List<ArticlePurchasesDTO> getPurchasesByLanguage(Long userId, String language) {
    List<ArticlePurchases> articlePurchases = articlePurchaseRepository.findByUserId(userId);
    List<ArticlePurchasesDTO> articlePurchasesDTO = new ArrayList<>();

    for(ArticlePurchases articlePurchase: articlePurchases) {
      Article article;
      if(language.equals("ko"))
        article = articleRepository.findById(articlePurchase.getArticleIdKo())
            .orElseThrow(() -> new NotFoundException("not found."));
      else
        article = articleRepository.findById(articlePurchase.getArticleIdEn())
            .orElseThrow(() -> new NotFoundException("not found."));

      ArticlePurchasesDTO articlePurchaseDTO = ArticlePurchasesDTO.builder()
          .id(articlePurchase.getId())
          .purchaseDate(articlePurchase.getPurchaseDate())
          .purchasePrice(articlePurchase.getPurchasePrice())
          .paymentStatus(articlePurchase.getPaymentStatus())
          .title(article.getTitle())
          .content(article.getContent())
          .category(article.getCategory())
          .language(article.getLanguage())
          .createdAt(article.getCreatedAt())
          .updatedAt(article.getUpdatedAt())
          .viewsCount(article.getViewCount())
          .slug(article.getSlug())
          .metaDescription(article.getMetaDescription())
          .build();

      articlePurchasesDTO.add(articlePurchaseDTO);
    }

    return articlePurchasesDTO;
  }

  @Transactional
  public Integer incrementViewCount(Long id) {
    Article article = articleRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Article not found"));
    article.incrementViewCount();
    return article.getViewCount();
  }

  public Integer getCurrentViewCount(Long articleId) {
    Article article = articleRepository.findById(articleId)
        .orElseThrow(() -> new EntityNotFoundException("Article not found"));

    return article.getViewCount();
  }
}