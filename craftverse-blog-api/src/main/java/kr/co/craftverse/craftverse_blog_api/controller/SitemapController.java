package kr.co.craftverse.craftverse_blog_api.controller;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.*;

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

  @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
  public void getSitemap(
      @RequestParam(name = "language", defaultValue = LANGUAGE_ALL) String language,
      HttpServletResponse response) throws IOException {

    response.setContentType("application/xml");
    response.setCharacterEncoding("UTF-8");

    StringBuilder sitemap = new StringBuilder();
    sitemap.append(SITEMAP_XML_HEADER);
    sitemap.append(SITEMAP_URLSET_OPEN);

    // 홈페이지
    addUrlToSitemap(sitemap, BASE_URL, PRIORITY_HOME, CHANGEFREQ_DAILY);

    // 언어별 홈페이지
    if (LANGUAGE_ALL.equals(language) || LANGUAGE_KO.equals(language)) {
      addUrlToSitemap(sitemap, BASE_URL + LANGUAGE_PATH_KO, PRIORITY_LANGUAGE_HOME, CHANGEFREQ_DAILY);
    }
    if (LANGUAGE_ALL.equals(language) || LANGUAGE_EN.equals(language)) {
      addUrlToSitemap(sitemap, BASE_URL + LANGUAGE_PATH_EN, PRIORITY_LANGUAGE_HOME, CHANGEFREQ_DAILY);
    }

    // 카테고리 페이지들
    for (String category : CATEGORIES) {
      if (LANGUAGE_ALL.equals(language) || LANGUAGE_KO.equals(language)) {
        addUrlToSitemap(sitemap, BASE_URL + LANGUAGE_PATH_KO + URL_CATEGORY_PATTERN + category,
            PRIORITY_CATEGORY, CHANGEFREQ_WEEKLY);
      }
      if (LANGUAGE_ALL.equals(language) || LANGUAGE_EN.equals(language)) {
        addUrlToSitemap(sitemap, BASE_URL + LANGUAGE_PATH_EN + URL_CATEGORY_PATTERN + category,
            PRIORITY_CATEGORY, CHANGEFREQ_WEEKLY);
      }
    }

    // 아티클들
    List<ArticleDTO> articles;
    if (LANGUAGE_ALL.equals(language)) {
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
      sitemap.append("    <changefreq>").append(CHANGEFREQ_MONTHLY).append("</changefreq>\n");
      sitemap.append("    <priority>").append(PRIORITY_ARTICLE).append("</priority>\n");
      sitemap.append("  </url>\n");
    }

    sitemap.append(SITEMAP_URLSET_CLOSE);

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
    String languagePath = LANGUAGE_KO.equals(article.getLanguage()) ? LANGUAGE_PATH_KO : LANGUAGE_PATH_EN;

    // slug가 있으면 slug 사용, 없으면 ID 사용
    if (article.getSlug() != null && !article.getSlug().trim().isEmpty()) {
      try {
        String encodedSlug = URLEncoder.encode(article.getSlug(), StandardCharsets.UTF_8);
        return BASE_URL + languagePath + URL_ARTICLE_PATTERN + encodedSlug;
      } catch (Exception e) {
        // 인코딩 실패시 ID 사용
        return BASE_URL + languagePath + URL_ARTICLE_PATTERN + article.getId();
      }
    } else {
      return BASE_URL + languagePath + URL_ARTICLE_PATTERN + article.getId();
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