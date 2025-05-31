package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.security.CustomUserDetails;
import kr.co.craftverse.craftverse_blog_api.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class ArticleController {
  private final ArticleService articleService;

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
  public RestResult<Map<String, Object>> getById(@PathVariable @Valid @Positive long id) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("article", articleService.getById(id));
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

  @PostMapping("/article")
  public RestResult<Map<String, Object>> create(@Valid @RequestBody ArticleDTO articleDTO) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("article", articleService.create(articleDTO));
    return new RestResult<>(data);
  }

  @PatchMapping("/article/{id}")
  public RestResult<Map<String, Object>> update(@PathVariable("id") long id, @RequestBody ArticleDTO articleDTO)
      throws Exception {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("article", articleService.update(id, articleDTO));
    return new RestResult<>(data);
  }

  @DeleteMapping("/article/{id}")
  public RestResult<Map<String, Object>> delete(@PathVariable("id") long id) throws Exception {
    Map<String, Object> data = new LinkedHashMap<>();
    articleService.delete(id);
    data.put("success", "true");
    return new RestResult<>(data);
  }

  @PostMapping("/article/{id}/views")
  public RestResult<Map<String, Object>> incrementViews(@PathVariable Long id,
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
}
