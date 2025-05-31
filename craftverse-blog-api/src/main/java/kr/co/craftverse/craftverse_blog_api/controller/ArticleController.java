package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.security.CustomUserDetails;
import kr.co.craftverse.craftverse_blog_api.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
public class ArticleController {
  private final ArticleService articleService;

  @GetMapping("/articles")
  public RestResult<Map<String, Object>> getByLanguage(
      @RequestParam(name = "language", defaultValue = "en") String language) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("articles", articleService.getByLanguage(language));
    return new RestResult<>(data);
  }

  @GetMapping("/article/{id}")
  public RestResult<Map<String, Object>> getById(@PathVariable long id) {
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
    data.put("article-purchases", articleService.getPurchasesBylanguage(userId, language));
    return new RestResult<>(data);
  }

  @PostMapping("/article")
  public RestResult<Map<String, Object>> create(@RequestBody ArticleDTO articleDTO) {
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

    // 기존 쿠키 확인
    boolean isNewVisitor = !hasVisitorCookie(request);

    if (isNewVisitor) {
      // 새 방문자 - 쿠키 생성 + 조회수 증가
      createVisitorCookie(response);
      Integer viewCount = articleService.incrementViewCount(id);
      data.put("viewCount", viewCount);
      data.put("isNewView", true);
    } else {
      // 기존 방문자 - 현재 조회수만 반환
      Integer currentViewCount = articleService.getCurrentViewCount(id);
      data.put("viewCount", currentViewCount);
      data.put("isNewView", false);
    }

    return new RestResult<>(data);
  }

  private boolean hasVisitorCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("blog_visitor_id".equals(cookie.getName())) {
          return true; // 쿠키 존재
        }
      }
    }
    return false; // 쿠키 없음
  }

  private void createVisitorCookie(HttpServletResponse response) {
    String visitorId = UUID.randomUUID().toString();
    Cookie visitorCookie = new Cookie("blog_visitor_id", visitorId);
    visitorCookie.setMaxAge(365 * 24 * 60 * 60); // 1년
    visitorCookie.setHttpOnly(true);
    visitorCookie.setPath("/");
    response.addCookie(visitorCookie);
  }
}
