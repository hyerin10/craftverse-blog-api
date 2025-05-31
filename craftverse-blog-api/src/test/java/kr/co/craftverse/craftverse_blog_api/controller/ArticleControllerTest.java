package kr.co.craftverse.craftverse_blog_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.Arrays;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ArticleController.class)
@DisplayName("ArticleController 테스트")
class ArticleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ArticleService articleService;

  @Autowired
  private ObjectMapper objectMapper;

  // 테스트 데이터
  private List<ArticleDTO> validArticles;
  private List<ArticleDTO> invalidArticles;
  private List<ArticlePurchasesDTO> samplePurchases;

  @BeforeEach
  void setUp() {
    // 유효한 게시글 데이터
    validArticles = Arrays.asList(
        new ArticleDTO(1L, "Test Article 1", "Content 1", "tech", "en", false, System.currentTimeMillis(), System.currentTimeMillis(), 0, "test-1", "Meta 1"),
        new ArticleDTO(2L, "Test Article 2", "Content 2", "series", "ko", true, System.currentTimeMillis(), System.currentTimeMillis(), 5, "test-2", "Meta 2"),
        new ArticleDTO(3L, "Test Article 3", "Content 3", "tech", "en", false, System.currentTimeMillis(), System.currentTimeMillis(), 10, "test-3", "Meta 3")
    );

    // 유효성 검증 실패용 데이터
    invalidArticles = Arrays.asList(
        new ArticleDTO(null, null, "Content", "tech", "en", false, null, null, null, null, null), // title 누락
        new ArticleDTO(1L, "Title", "Content", "invalid", "en", false, null, null, null, null, null), // 잘못된 카테고리
        new ArticleDTO(2L, "Title", "Content", "tech", "fr", false, null, null, null, null, null) // 잘못된 언어
    );

    // 구매 데이터
    samplePurchases = Arrays.asList(
        new ArticlePurchasesDTO(1L, System.currentTimeMillis(), new BigDecimal("9.99"), "COMPLETED", "Premium 1", "Content 1", "tech", "en", true, System.currentTimeMillis(), System.currentTimeMillis(), 15, "premium-1", "Premium Meta 1"),
        new ArticlePurchasesDTO(2L, System.currentTimeMillis(), new BigDecimal("14.99"), "COMPLETED", "Premium 2", "Content 2", "series", "ko", true, System.currentTimeMillis(), System.currentTimeMillis(), 25, "premium-2", "Premium Meta 2")
    );
  }

  @Nested
  @DisplayName("200 OK - 성공 케이스")
  class Status200Tests {

    @Test
    @WithMockUser
    @DisplayName("GET /articles - 언어별 게시글 조회 성공")
    void test200_getArticlesByLanguage() throws Exception {
      String[] languages = {"en", "ko"};

      for (String language : languages) {
        when(articleService.getByLanguage(language)).thenReturn(validArticles);

        mockMvc.perform(get("/articles").param("language", language))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result.articles").isArray());

        verify(articleService).getByLanguage(language);
      }
    }

    @Test
    @WithMockUser
    @DisplayName("GET /article/{id} - 게시글 조회 성공")
    void test200_getArticleById() throws Exception {
      for (ArticleDTO article : validArticles) {
        when(articleService.getById(article.getId())).thenReturn(article);

        mockMvc.perform(get("/article/" + article.getId()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.article.id").value(article.getId()))
            .andExpect(jsonPath("$.result.article.title").value(article.getTitle()));

        verify(articleService).getById(article.getId());
      }
    }

    @Test
    @DisplayName("GET /article-purchases - 구매 목록 조회 성공")
    void test200_getArticlePurchases() throws Exception {
      when(articleService.getPurchasesByLanguage(1L, "en")).thenReturn(samplePurchases);

      mockMvc.perform(get("/article-purchases")
              .with(user(new CustomUserDetails(1L))))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.article-purchases").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /article - 게시글 생성 성공")
    void test200_createArticle() throws Exception {
      for (ArticleDTO article : validArticles) {
        when(articleService.create(any(ArticleDTO.class))).thenReturn(article);

        mockMvc.perform(post("/article")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(article)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.article.title").value(article.getTitle()));
      }
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /article/{id} - 게시글 수정 성공")
    void test200_updateArticle() throws Exception {
      for (ArticleDTO article : validArticles) {
        when(articleService.update(eq(article.getId()), any(ArticleDTO.class))).thenReturn(article);

        mockMvc.perform(patch("/article/" + article.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(article)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.article.title").value(article.getTitle()));
      }
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /article/{id} - 게시글 삭제 성공")
    void test200_deleteArticle() throws Exception {
      for (ArticleDTO article : validArticles) {
        doNothing().when(articleService).delete(article.getId());

        mockMvc.perform(delete("/article/" + article.getId()).with(csrf()))
            .andDo(print())
            .andExpect(status().isOk());

        verify(articleService).delete(article.getId());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("POST /article/{id}/views - 조회수 증가 성공")
    void test200_incrementViews() throws Exception {
      Long[] articleIds = {1L, 2L, 3L};

      for (Long id : articleIds) {
        when(articleService.incrementViewCount(id)).thenReturn(10);

        MvcResult result = mockMvc.perform(post("/article/" + id + "/views").with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.viewCount").value(10))
            .andReturn();

        Cookie cookie = result.getResponse().getCookie("article_viewed_" + id);
        assert cookie != null;
        assert cookie.getValue().equals("viewed");
      }
    }
  }

  @Nested
  @DisplayName("400 Bad Request - 잘못된 요청")
  class Status400Tests {

    @Test
    @WithMockUser
    @DisplayName("GET /articles - 지원하지 않는 언어")
    void test400_unsupportedLanguage() throws Exception {
      String[] unsupportedLanguages = {"fr", "de", "jp"};

      for (String language : unsupportedLanguages) {
        when(articleService.getByLanguage(language)).thenReturn(Arrays.asList());

        mockMvc.perform(get("/articles").param("language", language))
            .andDo(print())
            .andExpect(status().isBadRequest());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("GET /article/{id} - 잘못된 ID 형식")
    void test400_invalidIdFormat() throws Exception {
      String[] invalidIds = {"invalid", "-1", "0"};

      for (String invalidId : invalidIds) {
        mockMvc.perform(get("/article/" + invalidId))
            .andDo(print())
            .andExpect(status().isBadRequest());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("POST /article - 유효성 검증 실패")
    void test400_validationFailure() throws Exception {
      for (ArticleDTO invalidArticle : invalidArticles) {
        mockMvc.perform(post("/article")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidArticle)))
            .andDo(print())
            .andExpect(status().isBadRequest());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("POST /article - 잘못된 JSON 형식")
    void test400_invalidJson() throws Exception {
      String[] invalidJsons = {"invalid json", "{incomplete", "null"};

      for (String invalidJson : invalidJsons) {
        mockMvc.perform(post("/article")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andDo(print())
            .andExpect(status().isBadRequest());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /article/{id} - 잘못된 ID로 수정")
    void test400_updateWithInvalidId() throws Exception {
      // 실제로 400을 반환하는지 확인 필요 - 환경에 따라 다를 수 있음
      mockMvc.perform(patch("/article/invalid")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(validArticles.get(0))))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /article/{id} - 잘못된 ID로 삭제")
    void test400_deleteWithInvalidId() throws Exception {
      // 실제로 400을 반환하는지 확인 필요 - 환경에 따라 다를 수 있음
      mockMvc.perform(delete("/article/invalid").with(csrf()))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("403 Forbidden - 권한 없음")
  class Status403Tests {

    @Test
    @WithMockUser
    @DisplayName("POST /article - CSRF 토큰 누락")
    void test403_missingCsrf() throws Exception {
      for (ArticleDTO article : validArticles) {
        mockMvc.perform(post("/article")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(article)))
            .andDo(print())
            .andExpect(status().isForbidden());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /article/{id} - CSRF 토큰 누락")
    void test403_updateMissingCsrf() throws Exception {
      for (ArticleDTO article : validArticles) {
        mockMvc.perform(patch("/article/" + article.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(article)))
            .andDo(print())
            .andExpect(status().isForbidden());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /article/{id} - CSRF 토큰 누락")
    void test403_deleteMissingCsrf() throws Exception {
      for (ArticleDTO article : validArticles) {
        mockMvc.perform(delete("/article/" + article.getId()))
            .andDo(print())
            .andExpect(status().isForbidden());
      }
    }
  }

  @Nested
  @DisplayName("404 Not Found - 리소스 없음")
  class Status404Tests {

    @Test
    @WithMockUser
    @DisplayName("GET /article/{id} - 존재하지 않는 게시글")
    void test404_articleNotFound() throws Exception {
      Long[] nonExistentIds = {999L, 1000L, 9999L};

      for (Long id : nonExistentIds) {
        when(articleService.getById(id)).thenThrow(new NotFoundException("Article not found"));

        mockMvc.perform(get("/article/" + id))
            .andDo(print())
            .andExpect(status().isNotFound());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("GET /article/{id} - 특수문자 포함 URL")
    void test404_specialCharactersInUrl() throws Exception {
      String[] specialCharUrls = {"/?$%"};

      for (String specialChar : specialCharUrls) {
        mockMvc.perform(get("/article/" + specialChar))
            .andDo(print())
            .andExpect(status().isNotFound());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /article/{id} - 존재하지 않는 게시글 수정")
    void test404_updateNonExistentArticle() throws Exception {
      Long[] nonExistentIds = {999L, 1000L, 9999L};

      for (Long id : nonExistentIds) {
        when(articleService.update(eq(id), any(ArticleDTO.class)))
            .thenThrow(new NotFoundException("Article not found"));

        mockMvc.perform(patch("/article/" + id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validArticles.get(0))))
            .andDo(print())
            .andExpect(status().isNotFound());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /article/{id} - 존재하지 않는 게시글 삭제")
    void test404_deleteNonExistentArticle() throws Exception {
      Long[] nonExistentIds = {999L, 1000L, 9999L};

      for (Long id : nonExistentIds) {
        doThrow(new NotFoundException("Article not found")).when(articleService).delete(id);

        mockMvc.perform(delete("/article/" + id).with(csrf()))
            .andDo(print())
            .andExpect(status().isNotFound());
      }
    }

    @Test
    @WithMockUser
    @DisplayName("존재하지 않는 엔드포인트")
    void test404_nonExistentEndpoint() throws Exception {
      String[] nonExistentPaths = {"/nonexistent", "/invalid-path", "/wrong-endpoint"};

      for (String path : nonExistentPaths) {
        mockMvc.perform(get(path))
            .andDo(print())
            .andExpect(status().isNotFound());
      }
    }
  }

  @Nested
  @DisplayName("405 Method Not Allowed - 지원하지 않는 HTTP 메소드")
  class Status405Tests {

    @Test
    @WithMockUser
    @DisplayName("지원하지 않는 HTTP 메소드")
    void test405_unsupportedMethods() throws Exception {
      String[] endpoints = {"/articles", "/article/1", "/article-purchases"};

      for (String endpoint : endpoints) {
        mockMvc.perform(put(endpoint).with(csrf()))
            .andDo(print())
            .andExpect(status().isMethodNotAllowed());
      }
    }
  }

  @Nested
  @DisplayName("415 Unsupported Media Type - 지원하지 않는 컨텐츠 타입")
  class Status415Tests {

    @Test
    @WithMockUser
    @DisplayName("POST /article - Content-Type 누락")
    void test415_missingContentType() throws Exception {
      for (ArticleDTO article : validArticles) {
        mockMvc.perform(post("/article")
                .with(csrf())
                .content(objectMapper.writeValueAsString(article)))
            .andDo(print())
            .andExpect(status().isUnsupportedMediaType());
      }
    }
  }
}