package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SitemapController {

  private final ArticleService articleService;
  private static final String BASE_URL = "https://craftverse.co.kr"; // 실제 도메인으로 변경

  @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
  public void getSitemap(
      @RequestParam(name = "language", defaultValue = "all") String language,
      HttpServletResponse response) throws IOException {

    response.setContentType("application/xml");
    response.setCharacterEncoding("UTF-8");

    StringBuilder sitemap = new StringBuilder();
    sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

    // 홈페이지
    addUrlToSitemap(sitemap, BASE_URL, "1.0", "daily");

    // 언어별 홈페이지
    if ("all".equals(language) || "ko".equals(language)) {
      addUrlToSitemap(sitemap, BASE_URL + "/ko", "0.9", "daily");
    }
    if ("all".equals(language) || "en".equals(language)) {
      addUrlToSitemap(sitemap, BASE_URL + "/en", "0.9", "daily");
    }

    // 카테고리 페이지들
    String[] categories = {"overoll", "tech", "series"};
    for (String category : categories) {
      if ("all".equals(language) || "ko".equals(language)) {
        addUrlToSitemap(sitemap, BASE_URL + "/ko/category/" + category, "0.8", "weekly");
      }
      if ("all".equals(language) || "en".equals(language)) {
        addUrlToSitemap(sitemap, BASE_URL + "/en/category/" + category, "0.8", "weekly");
      }
    }

    // 아티클들
    List<ArticleDTO> articles;
    if ("all".equals(language)) {
      articles = articleService.getAllArticles(); // 모든 언어의 아티클
    } else {
      articles = articleService.getByLanguage(language);
    }

    for (ArticleDTO article : articles) {
      String articleUrl = buildArticleUrl(article);
      String lastmod = formatTimestamp(article.getUpdatedAt() != null ?
          article.getUpdatedAt() : article.getCreatedAt());

      sitemap.append("  <url>\n");
      sitemap.append("    <loc>").append(articleUrl).append("</loc>\n");
      if (lastmod != null) {
        sitemap.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
      }
      sitemap.append("    <changefreq>monthly</changefreq>\n");
      sitemap.append("    <priority>0.7</priority>\n");
      sitemap.append("  </url>\n");
    }

    sitemap.append("</urlset>");

    response.getWriter().write(sitemap.toString());
  }

  private void addUrlToSitemap(StringBuilder sitemap, String url, String priority, String changefreq) {
    sitemap.append("  <url>\n");
    sitemap.append("    <loc>").append(url).append("</loc>\n");
    sitemap.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
    sitemap.append("    <priority>").append(priority).append("</priority>\n");
    sitemap.append("  </url>\n");
  }

  private String buildArticleUrl(ArticleDTO article) {
    String languagePath = "ko".equals(article.getLanguage()) ? "/ko" : "/en";

    // slug가 있으면 slug 사용, 없으면 ID 사용
    if (article.getSlug() != null && !article.getSlug().trim().isEmpty()) {
      try {
        String encodedSlug = URLEncoder.encode(article.getSlug(), StandardCharsets.UTF_8);
        return BASE_URL + languagePath + "/article/" + encodedSlug;
      } catch (Exception e) {
        // 인코딩 실패시 ID 사용
        return BASE_URL + languagePath + "/article/" + article.getId();
      }
    } else {
      return BASE_URL + languagePath + "/article/" + article.getId();
    }
  }

  private String formatTimestamp(Long timestamp) {
    if (timestamp == null) return null;

    try {
      Instant instant = Instant.ofEpochMilli(timestamp);
      return DateTimeFormatter.ISO_INSTANT.format(instant);
    } catch (Exception e) {
      return null;
    }
  }
}