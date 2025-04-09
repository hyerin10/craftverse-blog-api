package kr.co.craftverse.craftverse_blog_api.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import kr.co.craftverse.craftverse_blog_api.common.exception.EmptyDataException;
import kr.co.craftverse.craftverse_blog_api.exception.ResourceNotFoundException;
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
  private final ViewCountService viewCountService;

  public List<ArticleDTO> getByLanguage(String language) {
    List<Article> articles = articleRepository.findByLanguage(language);

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

  public List<ArticlePurchasesDTO> getPurchasesBylanguage(Long userId, String language) {
    List<ArticlePurchases> articlePurchases = articlePurchaseRepository.findByUserId(userId);
    List<ArticlePurchasesDTO> articlePurchasesDTO = new ArrayList<>();

    for(ArticlePurchases articlePurchase: articlePurchases) {
      Article article;
      if(language.equals("ko"))
        article = articleRepository.findById(articlePurchase.getArticleIdKo())
            .orElseThrow(() -> new ResourceNotFoundException("not found."));
      else
        article = articleRepository.findById(articlePurchase.getArticleIdEn())
            .orElseThrow(() -> new ResourceNotFoundException("not found."));

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
          .viewsCount(article.getViewsCount())
          .slug(article.getSlug())
          .metaDescription(article.getMetaDescription())
          .build();

      articlePurchasesDTO.add(articlePurchaseDTO);
    }

    return articlePurchasesDTO;
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

  @Transactional
  public Integer incrementViewCount(Long id) {
    Article article = articleRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Article not found"));

    Integer newViewCount = article.incrementViewsCount();
    article.updateModifiedTime(System.currentTimeMillis());

    articleRepository.save(article);
    return newViewCount;
  }

  /**
   * 중복 조회 방지를 위한 조회수 증가 (세션 ID를 통한 방문자 구분)
   * @param articleId 게시글 ID
   * @param visitorIdentifier 방문자 식별자 (세션 ID 또는 IP)
   * @param expirationTimeHours 중복 조회 방지 시간(시간 단위)
   * @return 증가된 후의 조회수 또는 중복 방문이면 현재 조회수
   */
  @Transactional
  public Integer incrementViewCountWithDuplicatePrevention(Long articleId, String visitorIdentifier, int expirationTimeHours) {
    // Redis를 사용하여 최근 방문 기록 확인
    boolean recentlyVisited = viewCountService.hasRecentlyVisited(articleId, visitorIdentifier);
    if (!recentlyVisited){
      viewCountService.recordVisit(articleId, visitorIdentifier, expirationTimeHours);
      return incrementViewCount(articleId);
    } else {
      // 최근 방문 기록이 있으면 현재 조회수 반환
      Article article = articleRepository.findById(articleId)
          .orElseThrow(() -> new EntityNotFoundException("Article not found with id: " + articleId));
      return article.getViewsCount();
    }
  }
}