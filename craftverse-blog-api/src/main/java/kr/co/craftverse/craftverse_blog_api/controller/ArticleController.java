package kr.co.craftverse.craftverse_blog_api.controller;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.ARTICLE_VIEWED_COOKIE_PREFIX;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.COOKIE_MAX_AGE_ONE_YEAR;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.COOKIE_PATH_ROOT;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.COOKIE_VALUE_VIEWED;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticlePurchaseDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticlePurchaseRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.payment.PaymentConfirmRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.payment.PaymentResponseDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.Payment;
import kr.co.craftverse.craftverse_blog_api.service.ArticleService;
import kr.co.craftverse.craftverse_blog_api.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class ArticleController {
  private final ArticleService articleService;
  private final JwtTokenProvider jwtTokenProvider;
  private final TossPaymentService tossPaymentService;
  private final Logger logger;

  @PostMapping("/article/purchase")
  public RestResult<Map<String, Object>> purchaseArticle(
      @Valid @RequestBody ArticlePurchaseRequestDTO articlePurchaseRequestDTO,
      HttpServletRequest request) throws Exception {

    Map<String, Object> data = new LinkedHashMap<>();

    // 1. 토큰에서 사용자 ID 추출
    String token = jwtTokenProvider.resolveToken(request);
    if (token == null || !jwtTokenProvider.validateToken(token)) {
      throw new UnauthorizedException();
    }
    Long userId = jwtTokenProvider.getUserId(token);

    logger.info("프리미엄 아티클 구매 요청 - userId: {}, articleId: {}, paymentKey: {}",
        userId, articlePurchaseRequestDTO.getArticleId(), articlePurchaseRequestDTO.getPaymentKey());

    // 2. 결제 승인 처리
    PaymentConfirmRequestDTO confirmDTO = new PaymentConfirmRequestDTO();
    confirmDTO.setPaymentKey(articlePurchaseRequestDTO.getPaymentKey());
    confirmDTO.setOrderId(articlePurchaseRequestDTO.getOrderId());
    confirmDTO.setAmount(articlePurchaseRequestDTO.getAmount());

    PaymentResponseDTO payment = tossPaymentService.confirmPayment(confirmDTO, userId);

    logger.info("결제 승인 완료 - userId: {}, paymentKey: {}, status: {}",
        userId, articlePurchaseRequestDTO.getPaymentKey(), payment.getStatus());

    // 3. 결제 상태 확인
    if (payment.getStatus() != Payment.PaymentStatus.DONE) {
      throw new IllegalStateException("결제 승인에 실패했습니다. 현재 상태: " + payment.getStatus());
    }

    // 4. 아티클 구매 처리
    ArticlePurchaseDTO purchaseResult = articleService.purchaseArticle(
        userId,
        articlePurchaseRequestDTO.getArticleId(),
        articlePurchaseRequestDTO.getLanguage(),
        articlePurchaseRequestDTO.getPaymentKey(),
        articlePurchaseRequestDTO.getOrderId()
    );

    // 5. 구매한 아티클 정보 조회
    ArticleDTO articleInfo = articleService.getById(articlePurchaseRequestDTO.getArticleId(), request);

    data.put("purchase", purchaseResult);
    data.put("article", articleInfo);
    data.put("payment", payment);
    data.put("message", "프리미엄 아티클 구매가 완료되었습니다.");
    data.put("success", true);

    logger.info("프리미엄 아티클 구매 완료 - userId: {}, articleId: {}, purchaseId: {}",
        userId, articlePurchaseRequestDTO.getArticleId(), purchaseResult.getId());

    return new RestResult<>(data);
  }

  @GetMapping("/articles")
  public RestResult<Map<String, Object>> getByLanguage(
      @RequestParam(name = "language", defaultValue = "en")
      @Pattern(regexp = "ko|en")
      String language) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("articles", articleService.getByLanguage(language));
    return new RestResult<>(data);
  }

  @GetMapping("/article/{id}")
  public RestResult<Map<String, Object>> getById(
      @PathVariable @Valid @Positive long id,
      HttpServletRequest request) { // HttpServletRequest 추가
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("article", articleService.getById(id, request)); // request 전달
    return new RestResult<>(data);
  }

  @GetMapping("/article/purchases")
  public RestResult<Map<String, Object>> getPurchases(
      @RequestParam(name = "language", defaultValue = "en") String language,
      HttpServletRequest request) {

    log.info("=== getPurchases 요청 시작 ===");

    // JWT 토큰에서 사용자 ID 추출
    String token = jwtTokenProvider.resolveToken(request);
    log.info("추출된 토큰: {}", token != null ? "존재함" : "null");

    if (token == null) {
      log.error("토큰이 없습니다.");
      throw new UnauthorizedException();
    }

    boolean isValid = jwtTokenProvider.validateToken(token);
    log.info("토큰 유효성: {}", isValid);

    if (!isValid) {
      log.error("유효하지 않은 토큰입니다.");
      throw new UnauthorizedException();
    }

    Long userId = jwtTokenProvider.getUserId(token);
    log.info("추출된 userId: {}", userId);

    if (userId == null) {
      log.error("토큰에서 userId를 추출할 수 없습니다.");
      throw new UnauthorizedException();
    }

    log.info("서비스 호출 전 - userId: {}, language: {}", userId, language);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("article-purchases", articleService.getPurchasesByLanguage(userId, language));
    return new RestResult<>(data);
  }

  @PostMapping("/article/{id}/views")
  public RestResult<Map<String, Object>> incrementViews(@PathVariable @Valid @Positive Long id,
      HttpServletRequest request,
      HttpServletResponse response) {

    Map<String, Object> data = new LinkedHashMap<>();

    // 해당 아티클에 대한 조회 쿠키 확인
    String articleCookieName = ARTICLE_VIEWED_COOKIE_PREFIX + id;
    boolean hasViewedThisArticle = hasArticleViewCookie(request, articleCookieName);

    if (!hasViewedThisArticle) {
      // 이 아티클을 처음 조회 - 쿠키 생성 + 조회수 증가
      createArticleViewCookie(response, articleCookieName);
      Integer viewCount = articleService.incrementViewCount(id);
      data.put("viewCount", viewCount);
      data.put("isNewView", true);
    } else {
      // 이미 조회한 아티클 - 현재 조회수만 반환
      Integer currentViewCount = articleService.getCurrentViewCount(id);
      data.put("viewCount", currentViewCount);
      data.put("isNewView", false);
    }

    return new RestResult<>(data);
  }

  private boolean hasArticleViewCookie(HttpServletRequest request, String cookieName) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookieName.equals(cookie.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  private void createArticleViewCookie(HttpServletResponse response, String cookieName) {
    Cookie viewCookie = new Cookie(cookieName, COOKIE_VALUE_VIEWED);
    viewCookie.setMaxAge(COOKIE_MAX_AGE_ONE_YEAR); // 1 year
    viewCookie.setHttpOnly(true);
    viewCookie.setPath(COOKIE_PATH_ROOT);
    response.addCookie(viewCookie);
  }

  @PostMapping("/article/{id}/expectations")
  public RestResult<Map<String, Object>> incrementExpectations(@PathVariable @Valid @Positive Long id,
      HttpServletRequest request,
      HttpServletResponse response) {

    Map<String, Object> data = new LinkedHashMap<>();

    // 해당 아티클에 대한 기대 쿠키 확인
    String articleExpectationCookieName = "article_expectation_" + id;
    boolean hasExpectedThisArticle = hasArticleExpectationCookie(request, articleExpectationCookieName);

    if (!hasExpectedThisArticle) {
      // 이 아티클을 처음 기대 - 쿠키 생성 + 기대수 증가
      createArticleExpectationCookie(response, articleExpectationCookieName);
      Integer expectationCount = articleService.incrementExpectationCount(id);
      data.put("expectationCount", expectationCount);
      data.put("isNewExpectation", true);
    } else {
      // 이미 기대한 아티클 - 현재 기대수만 반환
      Integer currentExpectationCount = articleService.getCurrentExpectationCount(id);
      data.put("expectationCount", currentExpectationCount);
      data.put("isNewExpectation", false);
    }

    return new RestResult<>(data);
  }

  private boolean hasArticleExpectationCookie(HttpServletRequest request, String cookieName) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookieName.equals(cookie.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  private void createArticleExpectationCookie(HttpServletResponse response, String cookieName) {
    Cookie expectationCookie = new Cookie(cookieName, "expected");
    expectationCookie.setMaxAge(COOKIE_MAX_AGE_ONE_YEAR); // 1 year
    expectationCookie.setHttpOnly(true);
    expectationCookie.setPath(COOKIE_PATH_ROOT);
    response.addCookie(expectationCookie);
  }
}