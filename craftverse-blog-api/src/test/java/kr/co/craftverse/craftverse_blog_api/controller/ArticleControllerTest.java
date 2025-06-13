package kr.co.craftverse.craftverse_blog_api.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.NotFoundException;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticlePurchasesDTO;
import kr.co.craftverse.craftverse_blog_api.security.CustomUserDetails;
import kr.co.craftverse.craftverse_blog_api.service.ArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ArticleController 테스트")
@Transactional
class ArticleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ArticleService articleService;

  @Autowired
  private ObjectMapper objectMapper;

  // 테스트 데이터
  private List<ArticleDTO> sampleArticles;
  private List<ArticlePurchasesDTO> samplePurchases;
  private ArticleDTO singleArticle;

  @BeforeEach
  void setUp() {
    // 샘플 게시글 데이터
    sampleArticles = Arrays.asList(
        new ArticleDTO(1L, "English Tech Article", "Content about technology", "tech", "en", false,
            System.currentTimeMillis(), System.currentTimeMillis(), 100, "english-tech", "Tech meta"),
        new ArticleDTO(2L, "Korean Series Article", "시리즈 콘텐츠", "series", "ko", true,
            System.currentTimeMillis(), System.currentTimeMillis(), 50, "korean-series", "시리즈 메타"),
        new ArticleDTO(3L, "Premium English Article", "Premium content", "premium", "en", true,
            System.currentTimeMillis(), System.currentTimeMillis(), 200, "premium-english", "Premium meta")
    );

    singleArticle = new ArticleDTO(1L, "Test Article", "Test Content", "tech", "en", false,
        System.currentTimeMillis(), System.currentTimeMillis(), 10, "test-article", "Test meta");

    // 구매 데이터
    samplePurchases = Arrays.asList(
        new ArticlePurchasesDTO(1L, System.currentTimeMillis(), new BigDecimal("9.99"), "COMPLETED",
            "Premium Article 1", "Premium content 1", "tech", "en", true,
            System.currentTimeMillis(), System.currentTimeMillis(), 150, "premium-1", "Premium meta 1"),
        new ArticlePurchasesDTO(2L, System.currentTimeMillis(), new BigDecimal("14.99"), "COMPLETED",
            "Premium Article 2", "Premium content 2", "series", "ko", true,
            System.currentTimeMillis(), System.currentTimeMillis(), 300, "premium-2", "Premium meta 2"),
        new ArticlePurchasesDTO(3L, System.currentTimeMillis(), new BigDecimal("19.99"), "PENDING",
            "Premium Article 3", "Premium content 3", "premium", "en", true,
            System.currentTimeMillis(), System.currentTimeMillis(), 500, "premium-3", "Premium meta 3")
    );
  }

  @Nested
  @DisplayName("GET /articles - 언어별 게시글 조회")
  @AutoConfigureMockMvc
  class GetArticlesByLanguageTests {

    @Test
    @WithMockUser
    @DisplayName("성공 - 한국어 게시글 조회")
    void success_getKoreanArticles() throws Exception {
      // Given
      List<ArticleDTO> koreanArticles = Collections.singletonList(sampleArticles.get(1));
      when(articleService.getByLanguage("ko")).thenReturn(koreanArticles);

      // When & Then
      mockMvc.perform(get("/articles").param("language", "ko"))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.result.articles").isArray())
          .andExpect(jsonPath("$.result.articles[0].language").value("ko"));

      verify(articleService).getByLanguage("ko");
    }

    @Test
    @WithMockUser
    @DisplayName("성공 - 기본 언어 (파라미터 없음)")
    void success_getDefaultLanguageArticles() throws Exception {
      // Given
      when(articleService.getByLanguage("en")).thenReturn(sampleArticles);

      // When & Then
      mockMvc.perform(get("/articles"))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.articles").isArray());

      verify(articleService).getByLanguage("en");
    }

    @Test
    @WithMockUser
    @DisplayName("성공 - 빈 결과")
    void success_getEmptyResults() throws Exception {
      // Given
      when(articleService.getByLanguage("en")).thenReturn(Collections.emptyList());

      // When & Then
      mockMvc.perform(get("/articles").param("language", "en"))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.articles").isArray())
          .andExpect(jsonPath("$.result.articles.length()").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("실패 - 지원하지 않는 언어")
    void fail_unsupportedLanguage() throws Exception {
      String[] unsupportedLanguages = {"fr", "de", "jp", "zh", "es", "invalid", "123"};

      for (String language : unsupportedLanguages) {
        mockMvc.perform(get("/articles").param("language", language))
            .andDo(print())
            .andExpect(status().isBadRequest());
      }
    }

    @Test
    @WithAnonymousUser
    @DisplayName("성공 - 익명 사용자도 조회 가능")
    void success_anonymousUserAccess() throws Exception {
      // Given
      when(articleService.getByLanguage("en")).thenReturn(sampleArticles);

      // When & Then
      mockMvc.perform(get("/articles")
              .param("language", "en")
              .contentType(MediaType.APPLICATION_JSON))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.articles").exists());
    }
  }

  @Nested
  @DisplayName("GET /article/{id} - 게시글 단건 조회")
  class GetArticleByIdTests {

    @Test
    @WithMockUser
    @DisplayName("성공 - 유효한 ID로 조회")
    void success_getValidId() throws Exception {
      // Given
      Long[] validIds = {1L, 2L, 3L, 100L, 999L, Long.MAX_VALUE};

      for (Long id : validIds) {
        ArticleDTO article = new ArticleDTO(id, "Test Article " + id, "Content", "tech", "en", false,
            System.currentTimeMillis(), System.currentTimeMillis(), 0, "test-" + id, "Meta");
        when(articleService.getById(id)).thenReturn(article);

        // When & Then
        mockMvc.perform(get("/article/" + id))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.article.id").value(id))
            .andExpect(jsonPath("$.result.article.title").value("Test Article " + id));

        verify(articleService).getById(id);
      }
    }

    @Test
    @WithMockUser
    @DisplayName("실패 - 잘못된 ID 형식")
    void fail_invalidIdFormat() throws Exception {
      String[] invalidIds = {"invalid", "abc", "null", "0", "-1", "-999", "1.5", "1a", "a1"};

      for (String invalidId : invalidIds) {
        mockMvc.perform(get("/article/" + invalidId))
            .andDo(print())
            .andExpect(status().isBadRequest());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("실패 - 존재하지 않는 게시글")
    void fail_articleNotFound() throws Exception {
      // Given
      Long[] nonExistentIds = {999L, 1000L, 9999L};

      for (Long id : nonExistentIds) {
        when(articleService.getById(id)).thenThrow(new NotFoundException());

        // When & Then
        mockMvc.perform(get("/article/" + id))
            .andDo(print())
            .andExpect(status().isNotFound());
      }
    }

    @Test
    @WithAnonymousUser
    @DisplayName("성공 - 익명 사용자도 조회 가능")
    void success_anonymousUserAccess() throws Exception {
      // Given
      when(articleService.getById(1L)).thenReturn(singleArticle);

      // When & Then
      mockMvc.perform(get("/article/1"))
          .andDo(print())
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("GET /article-purchases - 구매 목록 조회")
  class GetArticlePurchasesTests {

    @Test
    @DisplayName("성공 - 인증된 사용자의 구매 목록 조회 (기본 언어)")
    void success_authenticatedUserDefaultLanguage() throws Exception {
      // Given
      Long userId = 1L;
      when(articleService.getPurchasesByLanguage(userId, "en")).thenReturn(samplePurchases);

      // When & Then
      mockMvc.perform(get("/article-purchases")
              .with(user(new CustomUserDetails(userId))))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.article-purchases").isArray())
          .andExpect(jsonPath("$.result.article-purchases.length()").value(3));

      verify(articleService).getPurchasesByLanguage(userId, "en");
    }

    @Test
    @DisplayName("성공 - 인증된 사용자의 한국어 구매 목록 조회")
    void success_authenticatedUserKoreanLanguage() throws Exception {
      // Given
      Long userId = 2L;
      List<ArticlePurchasesDTO> koreanPurchases = Collections.singletonList(samplePurchases.get(1));
      when(articleService.getPurchasesByLanguage(userId, "ko")).thenReturn(koreanPurchases);

      // When & Then
      mockMvc.perform(get("/article-purchases")
              .param("language", "ko")
              .with(user(new CustomUserDetails(userId))))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.article-purchases[0].language").value("ko"));

      verify(articleService).getPurchasesByLanguage(userId, "ko");
    }

    @Test
    @DisplayName("성공 - 구매 내역이 없는 사용자")
    void success_noPurchases() throws Exception {
      // Given
      Long userId = 3L;
      when(articleService.getPurchasesByLanguage(userId, "en")).thenReturn(Collections.emptyList());

      // When & Then
      mockMvc.perform(get("/article-purchases")
              .with(user(new CustomUserDetails(userId))))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.article-purchases").isArray())
          .andExpect(jsonPath("$.result.article-purchases.length()").value(0));
    }

    @Test
    @DisplayName("성공 - 사용자")
    void success_authenticatedUser() throws Exception {
      // Given
      CustomUserDetails userDetails = new CustomUserDetails(1L);
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(authentication);

      List<ArticlePurchasesDTO> mockPurchases = Arrays.asList(
          new ArticlePurchasesDTO(1L, 1717430400000L, new BigDecimal("29.99"), "COMPLETED",
              "Advanced Spring Boot Techniques",
              "Learn advanced Spring Boot development patterns...", "tech", "en", true,
              1717344000000L, 1717430400000L, 1250, "advanced-spring-boot-techniques",
              "Comprehensive guide to advanced Spring Boot development"),
          new ArticlePurchasesDTO(2L, 1717516800000L, new BigDecimal("19.99"), "COMPLETED",
              "React Performance Optimization",
              "Master React performance optimization techniques...", "tech", "en", true,
              1717257600000L, 1717516800000L, 890, "react-performance-optimization",
              "Essential React performance tips and tricks")
      );

      when(articleService.getPurchasesByLanguage(1L, "en")).thenReturn(mockPurchases);

      // When & Then
      mockMvc.perform(get("/article-purchases")
              .param("language", "en"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.article-purchases").isArray());

      verify(articleService).getPurchasesByLanguage(1L, "en");
    }

    @Test
    @DisplayName("성공 - 다양한 사용자 ID")
    void success_variousUserIds() throws Exception {
      Long[] userIds = {1L, 100L, 999L, Long.MAX_VALUE};

      for (Long userId : userIds) {
        when(articleService.getPurchasesByLanguage(userId, "en")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/article-purchases")
                .with(user(new CustomUserDetails(userId))))
            .andDo(print())
            .andExpect(status().isOk());

        verify(articleService).getPurchasesByLanguage(userId, "en");
      }
    }

    @Test
    @DisplayName("성공 - 구매 상태별 다양한 데이터")
    void success_variousPurchaseStatuses() throws Exception {
      // Given
      Long userId = 1L;
      CustomUserDetails userDetails = new CustomUserDetails(userId);
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(authentication);

      List<ArticlePurchasesDTO> mixedStatusPurchases = Arrays.asList(
          new ArticlePurchasesDTO(1L, System.currentTimeMillis(), new BigDecimal("9.99"), "COMPLETED",
              "Completed Article", "Content", "tech", "en", true, System.currentTimeMillis(),
              System.currentTimeMillis(), 100, "completed", "Meta"),
          new ArticlePurchasesDTO(2L, System.currentTimeMillis(), new BigDecimal("14.99"), "PENDING",
              "Pending Article", "Content", "series", "en", true, System.currentTimeMillis(),
              System.currentTimeMillis(), 200, "pending", "Meta"),
          new ArticlePurchasesDTO(3L, System.currentTimeMillis(), new BigDecimal("19.99"), "FAILED",
              "Failed Article", "Content", "tech", "en", true, System.currentTimeMillis(),
              System.currentTimeMillis(), 300, "failed", "Meta")
      );

      when(articleService.getPurchasesByLanguage(userId, "en")).thenReturn(mixedStatusPurchases);

      // When & Then
      mockMvc.perform(get("/article-purchases")
              .param("language", "en"))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.article-purchases").isArray());

      verify(articleService).getPurchasesByLanguage(userId, "en");
    }

    @Test
    @DisplayName("실패 - 인증되지 않은 사용자의 구매 목록 조회 시 OAuth2 로그인으로 리다이렉트")
    void fail_unauthenticatedUserRedirectsToOAuth2Login() throws Exception {
      // Given
      // 인증되지 않은 상태 (user() 없이 요청)

      // When & Then
      mockMvc.perform(get("/article-purchases")
              .param("language", "ko"))
          .andDo(print())
          .andExpect(status().isFound()) // 302 Found (리다이렉트)
          .andExpect(redirectedUrlPattern("**/oauth2/authorization/google")); // OAuth2 로그인으로 리다이렉트

      // 서비스 메서드가 호출되지 않았는지 확인
      verifyNoInteractions(articleService);
    }
  }

  @Nested
  @DisplayName("POST /article/{id}/views - 조회수 증가")
  class IncrementViewsTests {

    @Test
    @WithMockUser
    @DisplayName("성공 - 첫 조회 (쿠키 없음)")
    void success_firstView() throws Exception {
      // Given
      Long[] articleIds = {1L, 2L, 3L, 100L, 999L};

      for (Long id : articleIds) {
        Integer ExpectViewCount = 10;
        when(articleService.incrementViewCount(id)).thenReturn(ExpectViewCount);

        // When & Then
        MvcResult result = mockMvc.perform(post("/article/" + id + "/views").with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.viewCount").value(ExpectViewCount))
            .andExpect(jsonPath("$.result.isNewView").value(true))
            .andExpect(cookie().value("article_viewed_" + id, "viewed"))
            .andExpect(cookie().maxAge("article_viewed_" + id, 365 * 24 * 60 * 60))
            .andExpect(cookie().httpOnly("article_viewed_" + id, true))
            .andExpect(cookie().path("article_viewed_" + id, "/"))
            .andReturn();

        verify(articleService).incrementViewCount(id);
        verify(articleService, never()).getCurrentViewCount(id);

        // 쿠키 확인
        Cookie cookie = result.getResponse().getCookie("article_viewed_" + id);
        assert cookie != null;
        assert cookie.getValue().equals("viewed");
        assert cookie.getMaxAge() == 365 * 24 * 60 * 60; // 1년
        assert cookie.isHttpOnly();
        assert cookie.getPath().equals("/");
      }
    }

    @Test
    @WithMockUser
    @DisplayName("성공 - 재조회 (쿠키 있음)")
    void success_repeatView() throws Exception {
      // Given
      Long articleId = 1L;
      Integer currentViewCount = 15;
      when(articleService.getCurrentViewCount(articleId)).thenReturn(currentViewCount);

      Cookie existingCookie = new Cookie("article_viewed_" + articleId, "viewed");

      // When & Then
      mockMvc.perform(post("/article/" + articleId + "/views")
              .with(csrf())
              .cookie(existingCookie))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.viewCount").value(currentViewCount))
          .andExpect(jsonPath("$.result.isNewView").value(false));

      verify(articleService, never()).incrementViewCount(articleId);
      verify(articleService).getCurrentViewCount(articleId);
    }

    @Test
    @WithMockUser
    @DisplayName("성공 - 다른 게시글 쿠키가 있는 경우")
    void success_differentArticleCookie() throws Exception {
      // Given
      Long articleId = 1L;
      Integer ExpectViewCount = 5;
      when(articleService.incrementViewCount(articleId)).thenReturn(ExpectViewCount);

      // 다른 게시글의 쿠키
      Cookie otherArticleCookie = new Cookie("article_viewed_2", "viewed");

      // When & Then
      mockMvc.perform(post("/article/" + articleId + "/views")
              .with(csrf())
              .cookie(otherArticleCookie))
          .andDo(print())
          .andExpect(status().isOk());

      verify(articleService).incrementViewCount(articleId);
    }

    @Test
    @WithMockUser
    @DisplayName("성공 - 여러 쿠키 중 해당 게시글 쿠키 확인")
    void success_multipleIncookies() throws Exception {
      // Given
      Long articleId = 3L;
      Integer currentViewCount = 25;
      when(articleService.getCurrentViewCount(articleId)).thenReturn(currentViewCount);

      Cookie[] multipleCookies = {
          new Cookie("article_viewed_1", "viewed"),
          new Cookie("article_viewed_2", "viewed"),
          new Cookie("article_viewed_3", "viewed"), // 해당 게시글 쿠키
          new Cookie("other_cookie", "value")
      };

      // When & Then
      mockMvc.perform(post("/article/" + articleId + "/views")
              .with(csrf())
              .cookie(multipleCookies))
          .andDo(print())
          .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("실패 - 잘못된 ID 형식")
    void fail_invalidIdFormat() throws Exception {
      String[] invalidIds = {"invalid", "abc", "null", "0", "-1", "1.5"};

      for (String invalidId : invalidIds) {
        mockMvc.perform(post("/article/" + invalidId + "/views").with(csrf()))
            .andDo(print())
            .andExpect(status().isBadRequest());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("실패 - CSRF 토큰 없음")
    void fail_missingCsrf() throws Exception {
      mockMvc.perform(post("/article/1/views"))
          .andDo(print())
          .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("성공 - 익명 사용자도 조회수 증가 가능")
    void success_anonymousUser() throws Exception {
      // Given
      Long articleId = 1L;
      when(articleService.incrementViewCount(articleId)).thenReturn(1);

      // When & Then
      mockMvc.perform(post("/article/" + articleId + "/views").with(csrf()))
          .andDo(print())
          .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("성공 - 조회수 0인 게시글")
    void success_zeroViewCount() throws Exception {
      // Given
      Long articleId = 1L;
      when(articleService.incrementViewCount(articleId)).thenReturn(1);

      // When & Then
      mockMvc.perform(post("/article/" + articleId + "/views").with(csrf()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.viewCount").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("성공 - 높은 조회수 게시글")
    void success_highViewCount() throws Exception {
      // Given
      Long articleId = 1L;
      Integer highViewCount = 999999;
      when(articleService.getCurrentViewCount(articleId)).thenReturn(highViewCount);

      Cookie existingCookie = new Cookie("article_viewed_" + articleId, "viewed");

      // When & Then
      mockMvc.perform(post("/article/" + articleId + "/views")
              .with(csrf())
              .cookie(existingCookie))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.viewCount").value(highViewCount));
    }

    @Test
    @WithMockUser
    @DisplayName("실패 - 존재하지 않는 게시글")
    void fail_articleNotFound() throws Exception {
      // Given
      Long nonExistentId = 999L;
      when(articleService.incrementViewCount(nonExistentId))
          .thenThrow(new NotFoundException());

      // When & Then
      mockMvc.perform(post("/article/" + nonExistentId + "/views").with(csrf()))
          .andDo(print())
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("405 Method Not Allowed - 지원하지 않는 HTTP 메소드")
  class MethodNotAllowedTests {

    @Test
    @WithMockUser
    @DisplayName("PUT 메소드 사용")
    void fail_putMethod() throws Exception {
      String[] endpoints = {"/articles", "/article/1", "/article-purchases", "/article/1/views"};

      for (String endpoint : endpoints) {
        mockMvc.perform(put(endpoint).with(csrf()))
            .andDo(print())
            .andExpect(status().isMethodNotAllowed());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE 메소드 사용")
    void fail_deleteMethod() throws Exception {
      String[] endpoints = {"/articles", "/article/1", "/article-purchases"};

      for (String endpoint : endpoints) {
        mockMvc.perform(delete(endpoint).with(csrf()))
            .andDo(print())
            .andExpect(status().isMethodNotAllowed());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH 메소드 사용")
    void fail_patchMethod() throws Exception {
      String[] endpoints = {"/articles", "/article/1", "/article-purchases", "/article/1/views"};

      for (String endpoint : endpoints) {
        mockMvc.perform(patch(endpoint).with(csrf()))
            .andDo(print())
            .andExpect(status().isMethodNotAllowed());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("POST 메소드를 GET 전용 엔드포인트에 사용")
    void fail_postMethodOnGetEndpoints() throws Exception {
      String[] getOnlyEndpoints = {"/articles", "/article/1", "/article-purchases"};

      for (String endpoint : getOnlyEndpoints) {
        mockMvc.perform(post(endpoint).with(csrf()))
            .andDo(print())
            .andExpect(status().isMethodNotAllowed());
      }
    }
  }

  @Nested
  @DisplayName("404 Not Found - 존재하지 않는 경로")
  class NotFoundTests {

    @Test
    @WithMockUser
    @DisplayName("존재하지 않는 엔드포인트")
    void fail_nonExistentEndpoints() throws Exception {
      String[] nonExistentPaths = {
          "/nonexistent",
          "/invalid-path",
          "/wrong-endpoint",
          "/article", // ID 없이 호출
          "/articles/1", // 잘못된 경로
          "/article-purchase" // 복수형 틀림
      };

      for (String path : nonExistentPaths) {
        mockMvc.perform(get(path))
            .andDo(print())
            .andExpect(status().isNotFound());
      }
      mockMvc.perform(post("/article/1/view"))
          .andDo(print())
          .andExpect(status().isForbidden());
    }

  @Nested
  @DisplayName("에지 케이스 및 경계값 테스트")
  class EdgeCaseTests {

    @Test
    @WithMockUser
    @DisplayName("매우 긴 쿠키 값")
    void success_longCookieValue() throws Exception {
      // Given
      Long articleId = 1L;
      StringBuilder longValue = new StringBuilder();
      for (int i = 0; i < 1000; i++) {
        longValue.append("a");
      }

      Cookie longCookie = new Cookie("article_viewed_" + articleId, longValue.toString());
      when(articleService.getCurrentViewCount(articleId)).thenReturn(10);

      // When & Then
      mockMvc.perform(post("/article/" + articleId + "/views")
              .with(csrf())
              .cookie(longCookie))
          .andDo(print())
          .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("최대값 ID 테스트")
    void success_maxValueId() throws Exception {
      // Given
      Long maxId = Long.MAX_VALUE;
      when(articleService.getById(maxId)).thenReturn(
          new ArticleDTO(maxId, "Max ID Article", "Content", "tech", "en", false,
              System.currentTimeMillis(), System.currentTimeMillis(), 0, "max-id", "Meta"));

      // When & Then
      mockMvc.perform(get("/article/" + maxId))
          .andDo(print())
          .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("최대값 초과 ID 테스트")
    void fail_overMaxValueId() throws Exception {
      // Given
      // Long.MAX_VALUE + 1은 오버플로우로 Long.MIN_VALUE가 됨
      Long overMaxId = Long.MAX_VALUE + 1; // -9223372036854775808
      when(articleService.getById(overMaxId))
          .thenThrow(new IllegalArgumentException("Invalid article ID: " + overMaxId));

      // When & Then
      mockMvc.perform(get("/article/" + overMaxId))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("null 파라미터 처리")
    void success_nullParameters() throws Exception {
      // Given
      when(articleService.getByLanguage("en")).thenReturn(sampleArticles);

      // When & Then - language 파라미터가 없으면 기본값 "en" 사용
      mockMvc.perform(get("/articles"))
          .andDo(print())
          .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("대소문자 구분 테스트")
    void fail_caseSensitiveLanguage() throws Exception {
      String[] caseSensitiveLanguages = {"EN", "KO", "En", "Ko", "eN", "kO"};

      for (String language : caseSensitiveLanguages) {
        mockMvc.perform(get("/articles").param("language", language))
            .andDo(print())
            .andExpect(status().isBadRequest());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("중복 파라미터")
    void fail_duplicateParameters() throws Exception {
      // Given
      when(articleService.getByLanguage("ko")).thenReturn(sampleArticles);

      // When & Then
      mockMvc.perform(get("/articles")
              .param("language", "en")
              .param("language", "ko"))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("매우 많은 쿠키")
    void success_manyCookies() throws Exception {
      // Given
      Long articleId = 1L;
      Cookie[] manyCookies = new Cookie[100];
      for (int i = 0; i < 100; i++) {
        manyCookies[i] = new Cookie("cookie_" + i, "value_" + i);
      }
      manyCookies[50] = new Cookie("article_viewed_" + articleId, "viewed"); // 해당 쿠키 포함

      when(articleService.getCurrentViewCount(articleId)).thenReturn(5);

      // When & Then
      mockMvc.perform(post("/article/" + articleId + "/views")
              .with(csrf())
              .cookie(manyCookies))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.isNewView").value(false));
    }
  }

  @Nested
  @DisplayName("성능 및 부하 관련 테스트")
  class PerformanceTests {

    @Test
    @WithMockUser
    @DisplayName("대량 데이터 응답")
    void success_largeDataResponse() throws Exception {
      // Given
      List<ArticleDTO> largeArticleList = Arrays.asList(new ArticleDTO[1000]);
      for (int i = 0; i < 1000; i++) {
        largeArticleList.set(i, new ArticleDTO((long) i, "Article " + i, "Content " + i,
            "tech", "en", false, System.currentTimeMillis(), System.currentTimeMillis(),
            i, "article-" + i, "Meta " + i));
      }
      when(articleService.getByLanguage("en")).thenReturn(largeArticleList);

      // When & Then
      mockMvc.perform(get("/articles").param("language", "en"))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.articles.length()").value(1000));
    }

    @Test
    @WithMockUser
    @DisplayName("동시 요청 시뮬레이션")
    void success_concurrentRequests() throws Exception {
      // Given
      when(articleService.getByLanguage("en")).thenReturn(sampleArticles);
      when(articleService.getById(1L)).thenReturn(singleArticle);

      // When & Then - 여러 요청을 빠르게 실행
      for (int i = 0; i < 10; i++) {
        mockMvc.perform(get("/articles"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/article/1"))
            .andExpect(status().isOk());
      }
    }
  }

  @Nested
  @DisplayName("데이터 무결성 테스트")
  class DataIntegrityTests {

    @Test
    @WithMockUser
    @DisplayName("최대 정수값 조회수")
    void success_maxIntegerViewCount() throws Exception {
      // Given
      when(articleService.incrementViewCount(1L)).thenReturn(Integer.MAX_VALUE);

      // When & Then
      mockMvc.perform(post("/article/1/views").with(csrf()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.viewCount").value(Integer.MAX_VALUE));
    }

    @Test
    @WithMockUser
    @DisplayName("조회수 오버플로우 처리")
    void fail_viewCountOverflow() throws Exception {
      // Given - MAX_VALUE에서 한 번 더 증가 시도
      when(articleService.incrementViewCount(1L))
          .thenThrow(new ArithmeticException("View count overflow"));

      // When & Then
      mockMvc.perform(post("/article/1/views").with(csrf()))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("국제화 및 다국어 테스트")
  class InternationalizationTests {

    @Test
    @WithMockUser
    @DisplayName("다양한 언어 코드 패턴 (유효하지 않음)")
    void fail_variousLanguageCodePatterns() throws Exception {
      String[] invalidLanguageCodes = {
          "eng", "kor", "english", "korean", "us",
          "en-US", "ko-KR", "zh-CN", "ja-JP"
      };

      for (String langCode : invalidLanguageCodes) {
        mockMvc.perform(get("/articles").param("language", langCode))
            .andDo(print())
            .andExpect(status().isBadRequest());
      }
    }
  }
  }
}
