package kr.co.craftverse.craftverse_blog_api.service;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.ARTICLE_FILE_PATH_PREFIX_WINDOWS;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.CACHE_EXPIRE_HOURS;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.CONTENT_TRUNCATION_SUFFIX;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.FILE_EXT_PNG;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.FILE_EXT_ZIP;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.HTML_TAG_REGEX;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.LANGUAGE_EN;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.LANGUAGE_KO;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PAYMENT_METHOD_CARD;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PAYMENT_STATUS_COMPLETE;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PAYMENT_STATUS_COMPLETED;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PAYMENT_STATUS_PAID;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PAYMENT_STATUS_SUCCESS;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PREVIEW_CONTENT_RATIO;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PURCHASE_CACHE_PREFIX;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.WHITESPACE_REGEX;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.NotFoundException;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticlePurchaseDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.Article;
import kr.co.craftverse.craftverse_blog_api.model.entity.ArticlePurchase;
import kr.co.craftverse.craftverse_blog_api.model.entity.ArticleTranslation;
import kr.co.craftverse.craftverse_blog_api.repository.ArticlePurchasesRepository;
import kr.co.craftverse.craftverse_blog_api.repository.ArticleRepository;
import kr.co.craftverse.craftverse_blog_api.repository.ArticleTranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
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
  private final ArticleTranslationRepository articleTranslationRepository;

  /**
   * 아티클 단건 조회 - 토큰 여부에 따라 콘텐츠 제한
   * 1. 토큰 없음: 프리미엄 아티클의 경우 30%만 제공
   * 2. 토큰 있음: 구매 여부 확인 후 전체/30% 제공
   * 3. 토큰이 블랙리스트에 있거나 만료된 경우: 비로그인 사용자로 처리
   */
  public ArticleDTO getById(long id, HttpServletRequest request) {
    return getById(id, request, false);
  }
  // todo: 유료 콘텐츠 접근 제한 구현 필요
  public ArticleDTO getById(long id, HttpServletRequest request, boolean ignoreCache) {
    Article article = articleRepository.findById(id).orElseThrow(NotFoundException::new);

    log.info("=== Article Detail Processing ===");
    log.info("Article ID: {}, Title: {}, isPremium: {}, language: {}",
        article.getId(), article.getTitle(), article.getIsPremium(), article.getLanguage());

    // JWT 토큰에서 사용자 ID 추출 시도 (블랙리스트/만료 확인 포함)
    Long userId = extractUserIdFromRequest(request);
    log.info("Extracted userId from token: {}", userId);

    // 콘텐츠 접근 권한 결정
    boolean isFullContentAvailable = true;
    boolean hasPremiumAccess = false;

    return ArticleDTO.builder()
        .id(article.getId())
        .title(article.getTitle())
        .category(article.getCategory())
        .language(article.getLanguage())
        .isPremium(article.getIsPremium())
        .premiumPrice(article.getPremiumPrice())
        .createdAt(article.getCreatedAt())
        .updatedAt(article.getUpdatedAt())
        .viewCount(article.getViewCount())
        .expectationCount(article.getExpectationCount())
        .slug(article.getSlug())
        .metaDescription(article.getMetaDescription())
        .isFullContentAvailable(isFullContentAvailable)
        .hasPremiumAccess(hasPremiumAccess)
        .isContentFiltered(!isFullContentAvailable)
        .slideCount(article.getSlideCount())
        .build();
  }

  public Resource getSlide(long id, int number, HttpServletRequest request)
      throws AccessDeniedException, FileNotFoundException {
    // 1. 기사 조회
    Article article = articleRepository.findById(id)
        .orElseThrow(NotFoundException::new);

    // 2. 프리미엄 기사 체크 및 권한 확인
    // number가 3 초과면서 프리미엄 기사인 경우에만 권한 체크
    if (number > 3 && article.getIsPremium()) {
      if (!checkPurchaseFromDatabase(extractUserIdFromRequest(request), id, "ko"))
        throw new AccessDeniedException("You don't have access to this article");
    }
    String paddedId = padZero(id);
    String paddedNumber = padZero(number);
    String filePath = ARTICLE_FILE_PATH_PREFIX_WINDOWS + paddedId + "\\" + paddedNumber + FILE_EXT_PNG;

    // 4. 파일 존재 여부 확인
    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      log.error("File not found at path: {}", filePath);
      throw new FileNotFoundException("File not found at: " + filePath);
    }

    // 5. PathResource 반환
    return new PathResource(path);

  }
  public Resource downloadZipFile(long id, HttpServletRequest request)
      throws FileNotFoundException, AccessDeniedException {
    // 1. 기사 조회
    Article article = articleRepository.findById(id)
        .orElseThrow(NotFoundException::new);

    // 2. 프리미엄 기사 체크 및 권한 확인
    if(article.getIsPremium() && !checkPurchaseFromDatabase(extractUserIdFromRequest(request), id, "ko"))
      throw new AccessDeniedException("You don't have access to this article");

    String paddedId = padZero(id);
    String filePath = ARTICLE_FILE_PATH_PREFIX_WINDOWS + paddedId + "\\" + paddedId + FILE_EXT_ZIP;

    // 4. 파일 존재 여부 확인
    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      log.error("File not found at path: {}", filePath);
      throw new FileNotFoundException("File not found at: " + filePath);
    }

    // 5. PathResource 반환
    return new PathResource(path);
  }

  // 제로 패딩 함수
  private String padZero(long id) {
    return String.format("%0" + 2 + "d", id);
  }

  /**
   * HTTP 요청에서 JWT 토큰을 통해 사용자 ID 추출
   * JwtTokenProvider의 기능을 최대한 활용하여 안전하게 사용자 정보 추출
   * JWT 예외는 GlobalExceptionHandler에서 처리됨
   */
  private Long extractUserIdFromRequest(HttpServletRequest request) {
    // JwtTokenProvider의 resolveToken 메서드 사용
    String token = jwtTokenProvider.resolveToken(request);
    log.debug("JWT Token extraction: token present = {}", token != null);

    if (token == null) {
      log.debug("No JWT Token found in request headers");
      return null;
    }

    // JwtTokenProvider의 validateToken 메서드 사용 (블랙리스트 확인 포함)
    // JWT 관련 예외(ExpiredJwtException, MalformedJwtException 등)는
    // GlobalExceptionHandler에서 401 응답으로 처리됨
    boolean isValid = jwtTokenProvider.validateToken(token);
    log.debug("JWT Token validation result: {}", isValid);

    if (!isValid) {
      log.debug("JWT Token is invalid or blacklisted");
      return null;
    }

    // JwtTokenProvider의 getUserId, getEmail 메서드 사용
    Long userId = jwtTokenProvider.getUserId(token);
    String email = jwtTokenProvider.getEmail(token);

    log.debug("Successfully extracted from JWT: userId={}, email={}", userId, email);
    return userId;
  }

  /**
   * 구매 여부 확인 (캐시 활용)
   */
  private boolean hasPurchasedArticle(Long userId, Long articleId, String language, boolean ignoreCache) {
    log.debug("=== Purchase Check Started ===");
    log.debug("Checking purchase: userId={}, articleId={}, language={}, ignoreCache={}",
        userId, articleId, language, ignoreCache);

    String cacheKey = PURCHASE_CACHE_PREFIX + userId + ":" + articleId + ":" + language;

    try {
      if (!ignoreCache) {
        // 캐시에서 먼저 확인
        Boolean cachedResult = (Boolean) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
          log.debug("CACHE HIT: result={}", cachedResult);
          return cachedResult;
        }
        log.debug("CACHE MISS - checking database");
      } else {
        log.debug("IGNORING CACHE - checking database directly");
      }

      // DB 조회
      boolean hasPurchased = checkPurchaseFromDatabase(userId, articleId, language);

      // 결과를 캐시에 저장
      redisTemplate.opsForValue().set(cacheKey, hasPurchased, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
      log.debug("Stored in cache: result={}", hasPurchased);

      return hasPurchased;

    } catch (Exception e) {
      log.error("Redis error during purchase check, falling back to DB: {}", e.getMessage());
      return checkPurchaseFromDatabase(userId, articleId, language);
    }
  }

  private boolean hasPurchasedArticle(Long userId, Long articleId, String language) {
    return hasPurchasedArticle(userId, articleId, language, false);
  }

  /**
   * 데이터베이스에서 구매 여부 확인
   */
  private boolean checkPurchaseFromDatabase(Long userId, Long articleId, String language) {
    log.debug("=== Database Purchase Check ===");
    log.debug("Query parameters: userId={}, articleId={}, language={}", userId, articleId, language);

    if(userId == null)
      throw new UnauthorizedException();

    try {
      List<ArticlePurchase> purchases = articlePurchasesRepository.findByUserId(userId);
      log.debug("Found {} total purchases for userId: {}", purchases.size(), userId);

      boolean hasPurchased = purchases.stream().anyMatch(purchase -> {
        boolean articleMatches = false;
        boolean statusMatches = false;

        // 언어별 아티클 ID 매칭 확인
        if (LANGUAGE_KO.equals(language)) {
          articleMatches = articleId.equals(purchase.getArticleIdKo());
        } else {
          articleMatches = articleId.equals(purchase.getArticleIdEn());
        }

        // 결제 상태 확인 (완료 상태들)
        String paymentStatus = purchase.getPaymentStatus();
        if (paymentStatus != null) {
          String normalizedStatus = paymentStatus.trim().toLowerCase();
          statusMatches = PAYMENT_STATUS_COMPLETED.equals(normalizedStatus) ||
              PAYMENT_STATUS_COMPLETE.equals(normalizedStatus) ||
              PAYMENT_STATUS_SUCCESS.equals(normalizedStatus) ||
              PAYMENT_STATUS_PAID.equals(normalizedStatus);
        }

        boolean matches = articleMatches && statusMatches;
        log.debug("Purchase match check: articleMatches={}, statusMatches={}, finalMatch={}",
            articleMatches, statusMatches, matches);

        return matches;
      });

      log.debug("=== Final Purchase Result: {} ===", hasPurchased);
      return hasPurchased;

    } catch (Exception e) {
      log.error("Error checking purchase from database: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 콘텐츠를 30%로 제한 (미리보기)
   */
  private String truncateContent(String content) {
    if (content == null || content.isEmpty()) {
      return content;
    }

    try {
      // HTML 태그 제거하여 텍스트만 추출
      String textOnly = content.replaceAll(HTML_TAG_REGEX, "");

      // 단어 단위로 자르기 (30%)
      String[] words = textOnly.split(WHITESPACE_REGEX);
      int targetWordCount = (int) (words.length * PREVIEW_CONTENT_RATIO);

      if (targetWordCount == 0) {
        targetWordCount = 1;
      }

      // 원본 HTML에서 단어 단위로 자르기
      StringBuilder truncated = new StringBuilder();
      String[] originalWords = content.split(WHITESPACE_REGEX);

      for (int i = 0; i < Math.min(targetWordCount, originalWords.length); i++) {
        if (i > 0) truncated.append(" ");
        truncated.append(originalWords[i]);
      }

      // 말줄임표 추가
      if (targetWordCount < originalWords.length) {
        truncated.append(CONTENT_TRUNCATION_SUFFIX);
      }

      log.debug("Content truncated: original={} chars, truncated={} chars",
          content.length(), truncated.length());

      return truncated.toString();
    } catch (Exception e) {
      log.error("Error truncating content: {}", e.getMessage());
      return content;
    }
  }

  // 캐시 관리 메서드들
  public void clearPurchaseCache(Long userId, Long articleId, String language) {
    String cacheKey = PURCHASE_CACHE_PREFIX + userId + ":" + articleId + ":" + language;
    try {
      Boolean deleted = redisTemplate.delete(cacheKey);
      log.info("Cache cleared: key={}, deleted={}", cacheKey, deleted);
    } catch (Exception e) {
      log.error("Failed to clear cache: {}", e.getMessage());
    }
  }

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

  // 기존 메서드들...
  /**
   * 특정 사용자와 아티클의 구매 정보 조회
   */
  public ArticlePurchaseDTO getPurchaseByUserAndArticle(Long userId, Long articleId) {
    log.info("구매 정보 조회 - userId: {}, articleId: {}", userId, articleId);

    // 한국어와 영어 아티클 모두에서 검색
    List<ArticlePurchase> purchases = articlePurchasesRepository.findByUserId(userId);

    Optional<ArticlePurchase> purchase = purchases.stream()
        .filter(p -> {
          // 한국어 또는 영어 아티클 ID가 일치하는지 확인
          boolean matchesKo = articleId.equals(p.getArticleIdKo());
          boolean matchesEn = articleId.equals(p.getArticleIdEn());

          // 결제 완료 상태인지 확인
          boolean isCompleted = PAYMENT_STATUS_COMPLETED.equals(p.getPaymentStatus()) ||
              PAYMENT_STATUS_COMPLETE.equals(p.getPaymentStatus()) ||
              PAYMENT_STATUS_SUCCESS.equals(p.getPaymentStatus()) ||
              PAYMENT_STATUS_PAID.equals(p.getPaymentStatus());

          return (matchesKo || matchesEn) && isCompleted;
        })
        .findFirst();

    if (purchase.isPresent()) {
      log.info("구매 정보 찾음 - purchaseId: {}", purchase.get().getId());
      return convertToDTO(purchase.get());
    } else {
      log.warn("구매 정보를 찾을 수 없음 - userId: {}, articleId: {}", userId, articleId);
      throw new NotFoundException();
    }
  }

  /**
   * 특정 언어의 구매 정보 조회 (언어별 조회)
   */
  public ArticlePurchaseDTO getPurchaseByUserAndArticle(Long userId, Long articleId, String language) {
    log.info("구매 정보 조회 (언어별) - userId: {}, articleId: {}, language: {}", userId, articleId, language);

    Optional<ArticlePurchase> purchase;

    if (LANGUAGE_KO.equals(language)) {
      purchase = articlePurchasesRepository.findByUserIdAndArticleIdKoAndCompleted(userId, articleId);
    } else {
      purchase = articlePurchasesRepository.findByUserIdAndArticleIdEnAndCompleted(userId, articleId);
    }

    if (purchase.isPresent()) {
      log.info("구매 정보 찾음 - purchaseId: {}", purchase.get().getId());
      return convertToDTO(purchase.get());
    } else {
      log.warn("구매 정보를 찾을 수 없음 - userId: {}, articleId: {}, language: {}", userId, articleId, language);
      throw new NotFoundException();
    }
  }

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
    Optional<ArticlePurchase> existingPurchase =
        articlePurchasesRepository.findByUserIdAndArticleIdAndLanguageAndCompleted(
            userId, articleId, language);

    if (existingPurchase.isPresent()) {
      // 이미 구매한 경우 기존 정보 반환
      log.info("이미 구매한 아티클입니다. userId: {}, articleId: {}", userId, articleId);
      return convertToDTO(existingPurchase.get());
    }

    // 5. 새로운 구매 기록 생성
    long currentTimestamp = System.currentTimeMillis();

    // 한/영 아티클 id 찾아서 같이 전달
    ArticleIds articleIds = findAllRelatedArticleIds(articleId);

    ArticlePurchase newPurchase = ArticlePurchase.builder()
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

    // 언어별 아티클 ID 설정 (한/영 모두 설정)
    setAllArticleIds(newPurchase, articleIds);

    // 6. 데이터베이스에 저장
    ArticlePurchase savedPurchase = articlePurchasesRepository.save(newPurchase);

    // 7. 캐시 무효화 (모든 관련 언어에 대해)
    clearPurchaseCacheForAllLanguages(userId, articleIds);

    log.info("프리미엄 아티클 구매 완료. userId: {}, articleId: {}, price: {}, paymentKey: {}, relatedIds: {}",
        userId, articleId, article.getPremiumPrice(), paymentKey, articleIds);

    return convertToDTO(savedPurchase);
  }

  /**
   * 주어진 아티클 ID와 연관된 모든 아티클 ID를 찾는 메서드
   */
  private ArticleIds findAllRelatedArticleIds(Long articleId) {
    // 1. 현재 아티클과 연관된 모든 번역 관계 찾기
    List<ArticleTranslation> translations = articleTranslationRepository.findAllTranslationsByArticleId(articleId);

    ArticleIds articleIds = new ArticleIds();

    // 2. 번역 관계가 없는 경우 (단일 언어 아티클)
    if (translations.isEmpty()) {
      // 아티클의 언어를 확인하여 적절한 필드에 설정
      Article article = articleRepository.findById(articleId).orElse(null);
      if (article != null) {
        String articleLanguage = determineArticleLanguage(article);
        if ("ko".equals(articleLanguage)) {
          articleIds.assignKoreanId(articleId);
        } else if ("en".equals(articleLanguage)) {
          articleIds.assignEnglishId(articleId);
        }
      }
      return articleIds;
    }

    // 3. 번역 관계가 있는 경우 모든 관련 ID 수집
    Set<Long> allRelatedIds = new HashSet<>();
    allRelatedIds.add(articleId);

    for (ArticleTranslation translation : translations) {
      allRelatedIds.add(translation.getOriginalArticleId());
      allRelatedIds.add(translation.getTranslatedArticleId());
    }

    // 4. 각 ID의 언어를 확인하여 분류
    for (Long id : allRelatedIds) {
      Article article = articleRepository.findById(id).orElse(null);
      if (article != null) {
        String language = determineArticleLanguage(article);
        if ("ko".equals(language)) {
          articleIds.assignKoreanId(id);
        } else if ("en".equals(language)) {
          articleIds.assignEnglishId(id);
        }
      }
    }

    return articleIds;
  }

  /**
   * 아티클의 언어를 결정하는 메서드
   */
  private String determineArticleLanguage(Article article) {
    // 방법 1: Article 엔티티에 language 필드가 있는 경우
    if (article.getLanguage() != null) {
      return article.getLanguage();
    }

    // 방법 2: 제목이나 내용으로 언어 감지 (간단한 방법)
    String title = article.getTitle();
    if (title != null) {
      // 한글 문자 포함 여부로 판단
      if (title.matches(".*[가-힣].*")) {
        return "ko";
      }
      // 영어로 가정
      return "en";
    }

    // 기본값
    return "ko";
  }

  /**
   * ArticlePurchase 엔티티에 모든 언어의 아티클 ID 설정
   */
  private void setAllArticleIds(ArticlePurchase purchase, ArticleIds articleIds) {
    if (articleIds.getKoreanId() != null) {
      purchase.setArticleIdKo(articleIds.getKoreanId());
    }
    if (articleIds.getEnglishId() != null) {
      purchase.setArticleIdEn(articleIds.getEnglishId());
    }
  }

  /**
   * 모든 관련 언어에 대해 캐시 무효화
   */
  private void clearPurchaseCacheForAllLanguages(Long userId, ArticleIds articleIds) {
    if (articleIds.getKoreanId() != null) {
      clearPurchaseCache(userId, articleIds.getKoreanId(), "ko");
    }
    if (articleIds.getEnglishId() != null) {
      clearPurchaseCache(userId, articleIds.getEnglishId(), "en");
    }
  }

  /**
   * 아티클 ID들을 담는 내부 클래스
   */
  private static class ArticleIds {
    private Long koreanId;
    private Long englishId;

    public Long getKoreanId() {
      return koreanId;
    }

    public void assignKoreanId(Long koreanId) {
      this.koreanId = koreanId;
    }

    public Long getEnglishId() {
      return englishId;
    }

    public void assignEnglishId(Long englishId) {
      this.englishId = englishId;
    }

    @Override
    public String toString() {
      return String.format("ArticleIds{korean=%d, english=%d}", koreanId, englishId);
    }
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
    List<ArticlePurchase> purchases =
        articlePurchasesRepository.findByUserIdAndPaymentStatusOrderByPurchaseDateDesc(userId, PAYMENT_STATUS_COMPLETED);

    return purchases.stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }

  /**
   * Entity를 DTO로 변환
   */
  private ArticlePurchaseDTO convertToDTO(ArticlePurchase purchase) {
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

  public List<ArticleDTO> getByLanguage(String language) {
    List<Article> articles = articleRepository.findByLanguage(language);

    if (articles.isEmpty())
      throw new NotFoundException();

    return articles.stream()
        .map(article -> ArticleDTO.builder()
            .id(article.getId())
            .title(article.getTitle())
            .category(article.getCategory())
            .language(article.getLanguage())
            .isPremium(article.getIsPremium())
            .createdAt(article.getCreatedAt())
            .updatedAt(article.getUpdatedAt())
            .viewCount(article.getViewCount())
            .expectationCount(article.getExpectationCount())
            .slug(article.getSlug())
            .metaDescription(article.getMetaDescription())
            .isFullContentAvailable(true)
            .slideCount(article.getSlideCount())
            .build())
        .collect(Collectors.toList());
  }

  @Transactional
  public Integer incrementViewCount(Long id) {
    Article article = articleRepository.findById(id)
        .orElseThrow(NotFoundException::new);
    article.incrementViewCount();
    return article.getViewCount();
  }

  public Integer getCurrentViewCount(Long articleId) {
    Article article = articleRepository.findById(articleId)
        .orElseThrow(NotFoundException::new);
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
            .category(article.getCategory())
            .language(article.getLanguage())
            .isPremium(article.getIsPremium())
            .createdAt(article.getCreatedAt())
            .updatedAt(article.getUpdatedAt())
            .viewCount(article.getViewCount())
            .slug(article.getSlug())
            .metaDescription(article.getMetaDescription())
            .isFullContentAvailable(true)
            .slideCount(article.getSlideCount())
            .build())
        .collect(Collectors.toList());
  }

  public List<ArticlePurchaseDTO> getPurchasesByLanguage(Long userId, String language) {
    log.info("getPurchasesByLanguage 호출 - userId: {}, language: {}", userId, language);

    List<ArticlePurchase> articlePurchases = articlePurchasesRepository.findByUserId(userId);
    log.info("찾은 구매 내역 수: {}", articlePurchases.size());

    List<ArticlePurchaseDTO> articlePurchasesDTO = new ArrayList<>();

    for(int i = 0; i < articlePurchases.size(); i++) {
      ArticlePurchase articlePurchase = articlePurchases.get(i);

      log.info("구매 내역 {}: id={}, articleIdKo={}, articleIdEn={}",
          i+1, articlePurchase.getId(),
          articlePurchase.getArticleIdKo(),
          articlePurchase.getArticleIdEn());

      Long articleId = null;
      if(LANGUAGE_KO.equals(language)) {
        articleId = articlePurchase.getArticleIdKo();
      } else {
        articleId = articlePurchase.getArticleIdEn();
      }

      log.info("선택된 articleId: {} (language: {})", articleId, language);

      if (articleId == null) {
        log.warn("Article ID가 null입니다. 구매 건 스킵: {}", articlePurchase.getId());
        continue;
      }

      Article article = articleRepository.findById(articleId)
          .orElseThrow(NotFoundException::new);

      ArticlePurchaseDTO articlePurchaseDTO = ArticlePurchaseDTO.builder()
          .id(articlePurchase.getId())
          .userId(articlePurchase.getUserId())
          .articleIdKo(articlePurchase.getArticleIdKo())
          .articleIdEn(articlePurchase.getArticleIdEn())
          .purchaseDate(articlePurchase.getPurchaseDate())
          .purchasePrice(articlePurchase.getPurchasePrice())
          .paymentStatus(articlePurchase.getPaymentStatus())
          .paymentKey(articlePurchase.getPaymentKey())
          .orderId(articlePurchase.getOrderId())
          .paymentMethod(articlePurchase.getPaymentMethod())
          .approvedAt(articlePurchase.getApprovedAt())
          .createdAt(articlePurchase.getCreatedAt())
          .updatedAt(articlePurchase.getUpdatedAt())
          .build();

      articlePurchasesDTO.add(articlePurchaseDTO);
    }

    log.info("반환할 구매 내역 수: {}", articlePurchasesDTO.size());
    return articlePurchasesDTO;
  }

  @Transactional
  public Integer incrementExpectationCount(Long id) {
    Article article = articleRepository.findById(id)
        .orElseThrow(NotFoundException::new);
    article.incrementExpectationCount();
    return article.getExpectationCount();
  }

  public Integer getCurrentExpectationCount(Long articleId) {
    Article article = articleRepository.findById(articleId)
        .orElseThrow(NotFoundException::new);
    return article.getExpectationCount();
  }
}