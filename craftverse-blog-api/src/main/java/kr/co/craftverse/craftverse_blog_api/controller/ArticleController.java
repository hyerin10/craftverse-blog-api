package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.NotFoundException;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticlePurchaseRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticlePurchasesDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.PaymentResponseDTO;
import kr.co.craftverse.craftverse_blog_api.security.CustomUserDetails;
import kr.co.craftverse.craftverse_blog_api.service.ArticleService;
import kr.co.craftverse.craftverse_blog_api.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
      @Valid @RequestBody ArticlePurchaseRequestDTO purchaseRequest,
      HttpServletRequest request) throws BadRequestException {

    Map<String, Object> data = new LinkedHashMap<>();

    try {
      // 1. 토큰에서 사용자 ID 추출
      Long userId = getUserIdFromToken(request);

      logger.info("프리미엄 아티클 구매 요청 - userId: {}, articleId: {}, paymentKey: {}",
          userId, purchaseRequest.getArticleId(), purchaseRequest.getPaymentKey());

      // 2. 결제 정보 검증 (TossPaymentService 활용)
      PaymentResponseDTO payment = tossPaymentService.getPaymentByKey(
          purchaseRequest.getPaymentKey(), userId);

      // 결제 상태 확인
//      if (!"DONE".equals(payment.getStatus())) {
//        throw new BadRequestException("결제가 완료되지 않았습니다. 현재 상태: " + payment.getStatus());
//      }

      // 주문 ID 검증
      if (!purchaseRequest.getOrderId().equals(payment.getOrderId())) {
        throw new BadRequestException("주문 ID가 일치하지 않습니다.");
      }

      // 결제 금액 검증
      if (!payment.getAmount().equals(purchaseRequest.getAmount())) {
        throw new BadRequestException("결제 금액이 일치하지 않습니다. " +
            "요청: " + purchaseRequest.getAmount() + ", 실제: " + payment.getAmount());
      }

      // 3. 아티클 구매 처리
      ArticlePurchasesDTO purchaseResult = articleService.purchaseArticle(
          userId,
          purchaseRequest.getArticleId(),
          purchaseRequest.getLanguage(),
          purchaseRequest.getPaymentKey(),
          purchaseRequest.getOrderId()
      );

      // 4. 구매한 아티클 정보 조회
      ArticleDTO articleInfo = articleService.getById(purchaseRequest.getArticleId(), request);

      data.put("purchase", purchaseResult);
      data.put("article", articleInfo);
      data.put("message", "프리미엄 아티클 구매가 완료되었습니다.");
      data.put("success", true);

      logger.info("프리미엄 아티클 구매 완료 - userId: {}, articleId: {}, purchaseId: {}",
          userId, purchaseRequest.getArticleId(), purchaseResult.getId());

      return new RestResult<>(data);

    } catch (UnauthorizedException e) {
      logger.error("인증 실패 - 로그인이 필요합니다", e);
      data.put("message", "로그인이 필요합니다.");
      data.put("success", false);
      throw e;

    } catch (BadRequestException e) {
      logger.error("잘못된 요청 - {}", e.getMessage(), e);
      data.put("message", e.getMessage());
      data.put("success", false);
      throw e;

    } catch (NotFoundException e) {
      logger.error("리소스를 찾을 수 없음 - {}", e.getMessage(), e);
      data.put("message", e.getMessage());
      data.put("success", false);
      throw e;

    } catch (Exception e) {
      logger.error("프리미엄 아티클 구매 처리 실패", e);

      String errorMessage = "아티클 구매 처리에 실패했습니다";
      if (e.getMessage() != null && !e.getMessage().isEmpty()) {
        errorMessage += ": " + e.getMessage();
      }

      data.put("message", errorMessage);
      data.put("success", false);
      data.put("error", e.getClass().getSimpleName());

      throw new RuntimeException(errorMessage, e);
    }
  }

  /**
   * 사용자의 구매한 프리미엄 아티클 목록 조회
   */
  @GetMapping("/article/purchases")
  public RestResult<Map<String, Object>> getUserPurchases(
      @RequestParam(defaultValue = "ko") String language,
      HttpServletRequest request) {

    Map<String, Object> data = new LinkedHashMap<>();

    try {
      Long userId = getUserIdFromToken(request);

      // 사용자의 모든 구매 기록 조회
      List<ArticlePurchasesDTO> purchases = articleService.getUserPremiumArticles(userId);

      // 언어별 필터링 (선택사항)
      List<ArticlePurchasesDTO> filteredPurchases = purchases.stream()
          .filter(purchase -> {
            Long articleId = purchase.getArticleIdByLanguage(language);
            return articleId != null;
          })
          .collect(Collectors.toList());

      data.put("purchases", filteredPurchases);
      data.put("totalCount", filteredPurchases.size());
      data.put("language", language);

      return new RestResult<>(data);

    } catch (Exception e) {
      log.error("구매 내역 조회 실패", e);
      data.put("message", "구매 내역을 조회할 수 없습니다: " + e.getMessage());
      throw new RuntimeException("구매 내역 조회에 실패했습니다", e);
    }
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

  @GetMapping("/article-purchases")
  public RestResult<Map<String, Object>> getPurchases(@RequestParam(name = "language", defaultValue = "en") String language) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Long userId = null;
    if (authentication != null && authentication.getPrincipal() instanceof UserDetails)
      userId = ((CustomUserDetails) authentication.getPrincipal()).getUserId();

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
    String articleCookieName = "article_viewed_" + id;
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
    Cookie viewCookie = new Cookie(cookieName, "viewed");
    viewCookie.setMaxAge(365 * 24 * 60 * 60); // 1 year
    viewCookie.setHttpOnly(true);
    viewCookie.setPath("/");
    response.addCookie(viewCookie);
  }

  /**
   * 토큰에서 사용자 ID 추출 (공통 메서드)
   */
  private Long getUserIdFromToken(HttpServletRequest request) {
    String token = jwtTokenProvider.resolveToken(request);
    if (token == null || !jwtTokenProvider.validateToken(token)) {
      throw new UnauthorizedException();
    }
    return jwtTokenProvider.getUserId(token);
  }
}
