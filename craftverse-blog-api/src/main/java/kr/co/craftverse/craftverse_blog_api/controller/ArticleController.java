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
import kr.co.craftverse.craftverse_blog_api.common.exception.http.NotFoundException;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticlePurchaseDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticlePurchaseRequestDTO;
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
      @Valid @RequestBody ArticlePurchaseRequestDTO articlePurchaseRequestDTO,
      HttpServletRequest request) throws BadRequestException {

    Map<String, Object> data = new LinkedHashMap<>();

    try {
      // 1. 토큰에서 사용자 ID 추출
      String token = jwtTokenProvider.resolveToken(request);
      Long userId = jwtTokenProvider.getUserId(token);

      logger.info("프리미엄 아티클 구매 요청 - userId: {}, articleId: {}, paymentKey: {}",
          userId, articlePurchaseRequestDTO.getArticleId(), articlePurchaseRequestDTO.getPaymentKey());

      // 2. 결제 정보 검증 (TossPaymentService 활용)
      PaymentResponseDTO payment = tossPaymentService.getPaymentByKey(
          articlePurchaseRequestDTO.getPaymentKey(), userId);

      // 결제 상태 확인
//      if (!"DONE".equals(payment.getStatus())) {
//        throw new BadRequestException("결제가 완료되지 않았습니다. 현재 상태: " + payment.getStatus());
//      }

      // 주문 ID 검증
      if (!articlePurchaseRequestDTO.getOrderId().equals(payment.getOrderId())) {
        throw new BadRequestException("주문 ID가 일치하지 않습니다.");
      }

      // 결제 금액 검증
      if (!payment.getAmount().equals(articlePurchaseRequestDTO.getAmount())) {
        throw new BadRequestException("결제 금액이 일치하지 않습니다. " +
            "요청: " + articlePurchaseRequestDTO.getAmount() + ", 실제: " + payment.getAmount());
      }

      // 3. 아티클 구매 처리
      ArticlePurchaseDTO purchaseResult = articleService.purchaseArticle(
          userId,
          articlePurchaseRequestDTO.getArticleId(),
          articlePurchaseRequestDTO.getLanguage(),
          articlePurchaseRequestDTO.getPaymentKey(),
          articlePurchaseRequestDTO.getOrderId()
      );

      // 4. 구매한 아티클 정보 조회
      ArticleDTO articleInfo = articleService.getById(articlePurchaseRequestDTO.getArticleId(), request);

      data.put("purchase", purchaseResult);
      data.put("article", articleInfo);
      data.put("message", "프리미엄 아티클 구매가 완료되었습니다.");
      data.put("success", true);

      logger.info("프리미엄 아티클 구매 완료 - userId: {}, articleId: {}, purchaseId: {}",
          userId, articlePurchaseRequestDTO.getArticleId(), purchaseResult.getId());

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
      //예외처리 구체적으로 필요
      throw new RuntimeException(errorMessage, e);
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

  @GetMapping("/article/purchases")
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
}
