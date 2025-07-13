package kr.co.craftverse.craftverse_blog_api.repository;

import kr.co.craftverse.craftverse_blog_api.model.entity.ArticleTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleTranslationRepository extends JpaRepository<ArticleTranslation, Long> {

  /**
   * 원본 아티클 ID로 번역된 아티클 찾기
   */
  List<ArticleTranslation> findByOriginalArticleId(Long originalArticleId);

  /**
   * 번역된 아티클 ID로 원본 아티클 찾기
   */
  List<ArticleTranslation> findByTranslatedArticleId(Long translatedArticleId);

  /**
   * 특정 아티클 ID에 대한 모든 번역 관계 찾기 (원본이든 번역이든)
   */
  @Query("SELECT at FROM ArticleTranslation at WHERE at.originalArticleId = :articleId OR at.translatedArticleId = :articleId")
  List<ArticleTranslation> findAllTranslationsByArticleId(@Param("articleId") Long articleId);
}