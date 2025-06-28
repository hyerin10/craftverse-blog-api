package kr.co.craftverse.craftverse_blog_api.service;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.NotFoundException;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticlePurchaseDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.Article;
import kr.co.craftverse.craftverse_blog_api.model.entity.ArticlePurchases;
import kr.co.craftverse.craftverse_blog_api.repository.ArticlePurchasesRepository;
import kr.co.craftverse.craftverse_blog_api.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {
  private final ArticleRepository articleRepository;
  private final ArticlePurchasesRepository articlePurchasesRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * 프리미엄 아티클 구매 처리 (가격 필드 사용)
   */
  public ArticlePurchaseDTO purchaseArticle(
      Long userId,
      Long articleId,
      String language,
      String paymentKey,
      String orderId) throws BadRequestException {

    // 1. 아티클 존재 여부 확인
    Article article = articleRepository.findById(articleId)
        .orElseThrow(() -> new NotFoundException());

    // 2. 프리미엄 아티클인지 확인
    if (!Boolean.TRUE.equals(article.getIsPremium()))
      throw new BadRequestException();

    // 3. 가격 정보 확인
    if (article.getPremiumPrice() == null || article.getPremiumPrice().compareTo(BigDecimal.ZERO) <= 0)
      throw new BadRequestException();

    // 4. 이미 구매한 아티클인지 확인 (중복 구매 방지)
    Optional<ArticlePurchases> existingPurchase =
        articlePurchasesRepository.findByUserIdAndArticleIdAndLanguageAndCompleted(
            userId, articleId, language);

    if (existingPurchase.isPresent()) {
      // 이미 구매한 경우 기존 정보 반환
      log.info("이미 구매한 아티클입니다. userId: {}, articleId: {}", userId, articleId);
      return convertToDTO(existingPurchase.get());
    }

    // 5. 새로운 구매 기록 생성
    long currentTimestamp = System.currentTimeMillis();

    ArticlePurchases newPurchase = ArticlePurchases.builder()
        .userId(userId)
        .paymentKey(paymentKey)
        .orderId(orderId)
        .purchasePrice(article.getPremiumPrice()) // BigDecimal 타입으로 저장
        .paymentStatus(PAYMENT_STATUS_COMPLETED)
        .paymentMethod(PAYMENT_METHOD_CARD) // 기본값, 필요시 파라미터로 받기
        .purchaseDate(currentTimestamp)
        .approvedAt(currentTimestamp)
        .createdAt(currentTimestamp)
        .updatedAt(currentTimestamp)
        .build();

    // 언어별 아티클 ID 설정
    newPurchase.setArticleIdByLanguage(language, articleId);

    // 6. 데이터베이스에 저장
    ArticlePurchases savedPurchase = articlePurchasesRepository.save(newPurchase);

    log.info("프리미엄 아티클 구매 완료. userId: {}, articleId: {}, price: {}, paymentKey: {}",
        userId, articleId, article.getPremiumPrice(), paymentKey);

    return convertToDTO(savedPurchase);
  }

  /**
   * 아티클 DTO 변환 시 프리미엄 정보 포함
   */
  private ArticleDTO convertToDTO(Article article, Long userId, String language) {
    ArticleDTO.ArticleDTOBuilder builder = ArticleDTO.builder()
        .id(article.getId())
        .title(article.getTitle())
        .content(article.getContent())
        .category(article.getCategory())
        .language(article.getLanguage())
        .isPremium(article.getIsPremium())
        .premiumPrice(article.getPremiumPrice()) // 가격 정보 포함
        .createdAt(article.getCreatedAt())
        .updatedAt(article.getUpdatedAt())
        .viewCount(article.getViewCount())
        .slug(article.getSlug())
        .metaDescription(article.getMetaDescription());

    // 프리미엄 아티클인 경우 접근 권한 확인
    if (Boolean.TRUE.equals(article.getIsPremium()) && userId != null) {
      boolean hasAccess = hasPremiumAccess(userId, article.getId(), language);
      builder.hasPremiumAccess(hasAccess);

      // 접근 권한이 없으면 콘텐츠 일부만 제공
      if (!hasAccess) {
        String filteredContent = applyContentFilter(article.getContent());
        builder.content(filteredContent)
            .isContentFiltered(true)
            .isFullContentAvailable(false);
      } else {
        builder.isFullContentAvailable(true);
      }
    } else if (Boolean.TRUE.equals(article.getIsPremium())) {
      // 비로그인 사용자는 프리미엄 콘텐츠 일부만 제공
      String filteredContent = applyContentFilter(article.getContent());
      builder.content(filteredContent)
          .hasPremiumAccess(false)
          .isContentFiltered(true)
          .isFullContentAvailable(false);
    } else {
      builder.isFullContentAvailable(true);
    }

    return builder.build();
  }

  /**
   * 프리미엄 콘텐츠 필터링 (30%만 제공)
   */
  private String applyContentFilter(String content) {
    if (content == null || content.isEmpty()) {
      return content;
    }

    int previewLength = (int) (content.length() * PREVIEW_CONTENT_RATIO); // 30%만 제공
    String filteredContent = content.substring(0, Math.min(previewLength, content.length()));

    // 마지막에 "..." 추가하여 더 있다는 것을 표시
    if (previewLength < content.length()) {
      filteredContent += CONTENT_TRUNCATION_SUFFIX;
    }

    return filteredContent;
  }

  /**
   * 사용자의 프리미엄 아티클 구매 여부 확인
   */
  public boolean hasPremiumAccess(Long userId, Long articleId, String language) {
    if (LANGUAGE_EN.equals(language)) {
      return articlePurchasesRepository.existsByUserIdAndArticleIdEnAndCompleted(userId, articleId);
    } else {
      return articlePurchasesRepository.existsByUserIdAndArticleIdKoAndCompleted(userId, articleId);
    }
  }

  /**
   * 사용자가 구매한 모든 프리미엄 아티클 조회
   */
  public List<ArticlePurchaseDTO> getUserPremiumArticles(Long userId) {
    List<ArticlePurchases> purchases =
        articlePurchasesRepository.findByUserIdAndPaymentStatusOrderByPurchaseDateDesc(userId, PAYMENT_STATUS_COMPLETED);

    return purchases.stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }

  /**
   * Entity를 DTO로 변환
   */
  private ArticlePurchaseDTO convertToDTO(ArticlePurchases purchase) {
    return ArticlePurchaseDTO.builder()
        .id(purchase.getId())
        .userId(purchase.getUserId())
        .articleIdKo(purchase.getArticleIdKo())
        .articleIdEn(purchase.getArticleIdEn())
        .purchaseDate(purchase.getPurchaseDate())
        .purchasePrice(purchase.getPurchasePrice())
        .paymentStatus(purchase.getPaymentStatus())
        .paymentKey(purchase.getPaymentKey())
        .orderId(purchase.getOrderId())
        .paymentMethod(purchase.getPaymentMethod())
        .approvedAt(purchase.getApprovedAt())
        .createdAt(purchase.getCreatedAt())
        .updatedAt(purchase.getUpdatedAt())
        .build();
  }

  public ArticleDTO getById(long id, HttpServletRequest request) {
    return getById(id, request, false); // 기본적으로 캐시 사용
  }

  public ArticleDTO getById(long id, HttpServletRequest request, boolean ignoreCache) {
    Article article = articleRepository.findById(id).orElseThrow(NotFoundException::new);

    log.info("=== Article Detail Processing ===");
    log.info("Article ID: {}, Title: {}, isPremium: {}, language: {}, ignoreCache: {}",
        article.getId(), article.getTitle(), article.getIsPremium(), article.getLanguage(), ignoreCache);

    // 사용자 ID 추출
    Long userId = extractUserIdFromRequest(request);
    log.info("Extracted userId from token: {}", userId);

    // 프리미엄 아티클인지 확인
    boolean isFullContentAvailable = true;
    String content = article.getContent();

    if (Boolean.TRUE.equals(article.getIsPremium())) {
      log.info("Article is PREMIUM - checking purchase status");

      if (userId == null) {
        log.info("User not authenticated - showing preview content");
        content = truncateContent(article.getContent());
        isFullContentAvailable = false;
      } else {
        boolean hasPurchased = hasPurchasedArticle(userId, id, article.getLanguage(), ignoreCache);
        log.info("Purchase check result: userId={}, articleId={}, language={}, hasPurchased={}",
            userId, id, article.getLanguage(), hasPurchased);

        if (!hasPurchased) {
          log.info("User has NOT purchased - showing preview content");
          content = truncateContent(article.getContent());
          isFullContentAvailable = false;
        } else {
          log.info("User has purchased - showing FULL content");
        }
      }
    } else {
      log.info("Article is NOT premium - showing full content");
    }

    log.info("Final content length: {}, isFullContentAvailable: {}",
        content.length(), isFullContentAvailable);

    return ArticleDTO.builder()
        .id(article.getId())
        .title(article.getTitle())
        .content(content)
        .category(article.getCategory())
        .language(article.getLanguage())
        .isPremium(article.getIsPremium())
        .createdAt(article.getCreatedAt())
        .updatedAt(article.getUpdatedAt())
        .viewCount(article.getViewCount())
        .slug(article.getSlug())
        .metaDescription(article.getMetaDescription())
        .isFullContentAvailable(isFullContentAvailable)
        .build();
  }

  private Long extractUserIdFromRequest(HttpServletRequest request) {
    try {
      String token = jwtTokenProvider.resolveToken(request);
      log.info("JWT Token extraction: token present = {}", token != null);

      if (token != null) {
        log.info("JWT Token (first 20 chars): {}", token.substring(0, Math.min(20, token.length())));

        boolean isValid = jwtTokenProvider.validateToken(token);
        log.info("JWT Token validation result: {}", isValid);

        if (isValid) {
          Long userId = jwtTokenProvider.getUserId(token);
          String email = jwtTokenProvider.getEmail(token);
          log.info("Successfully extracted from JWT: userId={}, email={}", userId, email);
          return userId;
        } else {
          log.warn("JWT Token is invalid");
        }
      } else {
        log.info("No JWT Token found in request");
      }
    } catch (Exception e) {
      log.error("Failed to extract user ID from token: {}", e.getMessage(), e);
    }
    return null;
  }

  /**
   * 구매 여부 확인 (캐시 무시 옵션 추가)
   */
  private boolean hasPurchasedArticle(Long userId, Long articleId, String language, boolean ignoreCache) {
    log.info("=== Purchase Check Started ===");
    log.info("Checking purchase: userId={}, articleId={}, language={}, ignoreCache={}",
        userId, articleId, language, ignoreCache);

    String cacheKey = PURCHASE_CACHE_PREFIX + userId + ":" + articleId + ":" + language;
    log.info("Cache key: {}", cacheKey);

    try {
      if (!ignoreCache) {
        // 캐시에서 먼저 확인
        Boolean cachedResult = (Boolean) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
          log.info("CACHE HIT: result={}", cachedResult);
          return cachedResult;
        }
        log.info("CACHE MISS - checking database");
      } else {
        log.info("IGNORING CACHE - checking database directly");
      }

      // DB 조회
      boolean hasPurchased = checkPurchaseFromDatabase(userId, articleId, language);

      // 결과를 캐시에 저장
      redisTemplate.opsForValue().set(cacheKey, hasPurchased, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
      log.info("Stored in cache: result={}", hasPurchased);

      return hasPurchased;

    } catch (Exception e) {
      log.error("Redis error during purchase check, falling back to DB: {}", e.getMessage(), e);
      return checkPurchaseFromDatabase(userId, articleId, language);
    }
  }

  // 기존 메서드 오버로드
  private boolean hasPurchasedArticle(Long userId, Long articleId, String language) {
    return hasPurchasedArticle(userId, articleId, language, false);
  }

  // 개선된 구매 확인 로직
  private boolean checkPurchaseFromDatabase(Long userId, Long articleId, String language) {
    log.info("=== Database Purchase Check ===");
    log.info("Query parameters: userId={}, articleId={}, language={}", userId, articleId, language);

    try {
      List<ArticlePurchases> purchases = articlePurchasesRepository.findByUserId(userId);
      log.info("Found {} total purchases for userId: {}", purchases.size(), userId);

      // 모든 구매 내역 상세 로깅
      for (int i = 0; i < purchases.size(); i++) {
        ArticlePurchases purchase = purchases.get(i);
        log.info("Purchase {}: id={}, articleIdKo={}, articleIdEn={}, paymentStatus='{}' (trimmed='{}')",
            i + 1, purchase.getId(), purchase.getArticleIdKo(),
            purchase.getArticleIdEn(), purchase.getPaymentStatus(),
            purchase.getPaymentStatus() != null ? purchase.getPaymentStatus().trim() : "null");
      }

      boolean hasPurchased = purchases.stream().anyMatch(purchase -> {
        boolean articleMatches = false;
        boolean statusMatches = false;
        Long targetArticleId = null;

        // 아티클 ID 매칭 확인
        if (LANGUAGE_KO.equals(language)) {
          targetArticleId = purchase.getArticleIdKo();
          articleMatches = articleId.equals(targetArticleId);
        } else {
          targetArticleId = purchase.getArticleIdEn();
          articleMatches = articleId.equals(targetArticleId);
        }

        // 결제 상태 확인 (대소문자 무시, 공백 제거)
        String paymentStatus = purchase.getPaymentStatus();
        if (paymentStatus != null) {
          String normalizedStatus = paymentStatus.trim().toLowerCase();
          // 다양한 완료 상태 허용
          statusMatches = PAYMENT_STATUS_COMPLETED.equals(normalizedStatus) ||
              PAYMENT_STATUS_COMPLETE.equals(normalizedStatus) ||
              PAYMENT_STATUS_SUCCESS.equals(normalizedStatus) ||
              PAYMENT_STATUS_PAID.equals(normalizedStatus);

          log.info("Payment status check: original='{}', normalized='{}', matches={}",
              paymentStatus, normalizedStatus, statusMatches);
        } else {
          log.info("Payment status is null");
        }

        boolean matches = articleMatches && statusMatches;

        log.info("Purchase match check: targetArticleId={} ({}), requestedArticleId={}, " +
                "articleMatches={}, statusMatches={}, finalMatch={}",
            targetArticleId, language, articleId, articleMatches, statusMatches, matches);

        return matches;
      });

      log.info("=== Final Purchase Result: {} ===", hasPurchased);
      return hasPurchased;

    } catch (Exception e) {
      log.error("Error checking purchase from database: {}", e.getMessage(), e);
      return false;
    }
  }

  // 캐시 무효화 유틸리티 메서드 추가
  public void clearPurchaseCache(Long userId, Long articleId, String language) {
    String cacheKey = PURCHASE_CACHE_PREFIX + userId + ":" + articleId + ":" + language;
    try {
      Boolean deleted = redisTemplate.delete(cacheKey);
      log.info("Cache cleared: key={}, deleted={}", cacheKey, deleted);
    } catch (Exception e) {
      log.error("Failed to clear cache: {}", e.getMessage());
    }
  }

  // 모든 사용자 구매 캐시 무효화
  public void clearAllPurchaseCacheForUser(Long userId) {
    try {
      String pattern = PURCHASE_CACHE_PREFIX + userId + ":*";
      var keys = redisTemplate.keys(pattern);
      if (keys != null && !keys.isEmpty()) {
        Long deletedCount = redisTemplate.delete(keys);
        log.info("Cleared {} cache entries for userId: {}", deletedCount, userId);
      }
    } catch (Exception e) {
      log.error("Failed to clear all purchase cache for user {}: {}", userId, e.getMessage());
    }
  }

  private String truncateContent(String content) {
    if (content == null || content.isEmpty()) {
      return content;
    }

    try {
      String textOnly = content.replaceAll(HTML_TAG_REGEX, "");
      int targetLength = (int) (textOnly.length() * PREVIEW_CONTENT_RATIO);

      String[] words = textOnly.split(WHITESPACE_REGEX);
      int targetWordCount = (int) (words.length * PREVIEW_CONTENT_RATIO);

      if (targetWordCount == 0) {
        targetWordCount = 1;
      }

      StringBuilder truncated = new StringBuilder();
      String[] originalWords = content.split(WHITESPACE_REGEX);

      for (int i = 0; i < Math.min(targetWordCount, originalWords.length); i++) {
        if (i > 0) truncated.append(" ");
        truncated.append(originalWords[i]);
      }

      log.info("Content truncated: original={} chars, truncated={} chars",
          content.length(), truncated.length());

      return truncated.toString();
    } catch (Exception e) {
      log.error("Error truncating content: {}", e.getMessage());
      return content;
    }
  }

  // 기존 메서드들...
  public List<ArticleDTO> getByLanguage(String language) {
    List<Article> articles = articleRepository.findByLanguage(language);

    if (articles.isEmpty())
      throw new NotFoundException();

    return articles.stream()
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
            .isFullContentAvailable(true)
            .build())
        .collect(Collectors.toList());
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

  public List<ArticleDTO> getAllArticles() {
    List<Article> articles = articleRepository.findAll();

    if (articles.isEmpty())
      throw new NotFoundException();

    return articles.stream()
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
            .isFullContentAvailable(true)
            .build())
        .collect(Collectors.toList());
  }

  public List<ArticlePurchaseDTO> getPurchasesByLanguage(Long userId, String language) {
    List<ArticlePurchases> articlePurchases = articlePurchasesRepository.findByUserId(userId);
    List<ArticlePurchaseDTO> articlePurchasesDTO = new ArrayList<>();

    for(ArticlePurchases articlePurchase: articlePurchases) {
      Article article;
      if(LANGUAGE_KO.equals(language))
        article = articleRepository.findById(articlePurchase.getArticleIdKo())
            .orElseThrow(NotFoundException::new);
      else
        article = articleRepository.findById(articlePurchase.getArticleIdEn())
            .orElseThrow(NotFoundException::new);

      ArticlePurchaseDTO articlePurchaseDTO = ArticlePurchaseDTO.builder()
          .id(articlePurchase.getId())
          .purchaseDate(articlePurchase.getPurchaseDate())
          .purchasePrice(articlePurchase.getPurchasePrice())
          .paymentStatus(articlePurchase.getPaymentStatus())
          .createdAt(article.getCreatedAt())
          .updatedAt(article.getUpdatedAt())
          .build();

      articlePurchasesDTO.add(articlePurchaseDTO);
    }

    return articlePurchasesDTO;
  }

  public void invalidatePurchaseCache(Long userId, Long articleIdKo, Long articleIdEn) {
    try {
      if (articleIdKo != null) {
        String cacheKeyKo = PURCHASE_CACHE_PREFIX + userId + ":" + articleIdKo + ":" + LANGUAGE_KO;
        redisTemplate.delete(cacheKeyKo);
      }
      if (articleIdEn != null) {
        String cacheKeyEn = PURCHASE_CACHE_PREFIX + userId + ":" + articleIdEn + ":" + LANGUAGE_EN;
        redisTemplate.delete(cacheKeyEn);
      }
      log.info("Purchase cache invalidated for userId: {}", userId);
    } catch (Exception e) {
      log.error("Failed to invalidate purchase cache: {}", e.getMessage());
    }
  }
}