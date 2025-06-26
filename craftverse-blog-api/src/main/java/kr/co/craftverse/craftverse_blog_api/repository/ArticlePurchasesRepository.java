package kr.co.craftverse.craftverse_blog_api.repository;

import kr.co.craftverse.craftverse_blog_api.model.entity.ArticlePurchases;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticlePurchasesRepository extends JpaRepository<ArticlePurchases, Long> {

  /**
   * 사용자의 모든 구매 기록 조회
   */
  List<ArticlePurchases> findByUserId(Long userId);

  /**
   * 사용자의 구매 기록을 구매일 순으로 조회 (최신순)
   */
  List<ArticlePurchases> findByUserIdOrderByPurchaseDateDesc(Long userId);

  /**
   * 결제 키로 구매 기록 조회
   */
  Optional<ArticlePurchases> findByPaymentKey(String paymentKey);

  /**
   * 주문 ID로 구매 기록 조회
   */
  Optional<ArticlePurchases> findByOrderId(String orderId);

  /**
   * 사용자의 특정 결제 상태 구매 기록 조회
   */
  List<ArticlePurchases> findByUserIdAndPaymentStatus(Long userId, String paymentStatus);

  /**
   * 사용자의 완료된 구매 기록만 조회 (최신순)
   */
  List<ArticlePurchases> findByUserIdAndPaymentStatusOrderByPurchaseDateDesc(
      Long userId, String paymentStatus);

  /**
   * 한국어 아티클 구매 여부 확인 (완료된 결제만)
   */
  @Query("SELECT COUNT(p) > 0 FROM ArticlePurchases p " +
      "WHERE p.userId = :userId AND p.articleIdKo = :articleId " +
      "AND p.paymentStatus = 'completed'")
  boolean existsByUserIdAndArticleIdKoAndCompleted(
      @Param("userId") Long userId,
      @Param("articleId") Long articleId);

  /**
   * 영어 아티클 구매 여부 확인 (완료된 결제만)
   */
  @Query("SELECT COUNT(p) > 0 FROM ArticlePurchases p " +
      "WHERE p.userId = :userId AND p.articleIdEn = :articleId " +
      "AND p.paymentStatus = 'completed'")
  boolean existsByUserIdAndArticleIdEnAndCompleted(
      @Param("userId") Long userId,
      @Param("articleId") Long articleId);

  /**
   * 사용자의 특정 아티클 구매 기록 조회 (언어별, 완료된 결제만)
   */
  @Query("SELECT p FROM ArticlePurchases p " +
      "WHERE p.userId = :userId " +
      "AND ((:language = 'ko' AND p.articleIdKo = :articleId) " +
      "     OR (:language = 'en' AND p.articleIdEn = :articleId)) " +
      "AND p.paymentStatus = 'completed'")
  Optional<ArticlePurchases> findByUserIdAndArticleIdAndLanguageAndCompleted(
      @Param("userId") Long userId,
      @Param("articleId") Long articleId,
      @Param("language") String language);

  /**
   * 사용자의 완료된 구매 개수 조회
   */
  long countByUserIdAndPaymentStatus(Long userId, String paymentStatus);

  /**
   * 특정 아티클의 구매자 수 조회 (한국어, 완료된 결제만)
   */
  @Query("SELECT COUNT(DISTINCT p.userId) FROM ArticlePurchases p " +
      "WHERE p.articleIdKo = :articleId AND p.paymentStatus = 'completed'")
  long countDistinctUsersByArticleIdKoAndCompleted(@Param("articleId") Long articleId);

  /**
   * 특정 아티클의 구매자 수 조회 (영어, 완료된 결제만)
   */
  @Query("SELECT COUNT(DISTINCT p.userId) FROM ArticlePurchases p " +
      "WHERE p.articleIdEn = :articleId AND p.paymentStatus = 'completed'")
  long countDistinctUsersByArticleIdEnAndCompleted(@Param("articleId") Long articleId);

  /**
   * 특정 기간 내 구매 기록 조회
   */
  @Query("SELECT p FROM ArticlePurchases p " +
      "WHERE p.purchaseDate >= :startDate AND p.purchaseDate <= :endDate " +
      "ORDER BY p.purchaseDate DESC")
  List<ArticlePurchases> findByPurchaseDateBetween(
      @Param("startDate") Long startDate,
      @Param("endDate") Long endDate);

  /**
   * 사용자의 특정 기간 내 구매 기록 조회
   */
  @Query("SELECT p FROM ArticlePurchases p " +
      "WHERE p.userId = :userId " +
      "AND p.purchaseDate >= :startDate AND p.purchaseDate <= :endDate " +
      "ORDER BY p.purchaseDate DESC")
  List<ArticlePurchases> findByUserIdAndPurchaseDateBetween(
      @Param("userId") Long userId,
      @Param("startDate") Long startDate,
      @Param("endDate") Long endDate);

  /**
   * 결제 상태별 구매 기록 조회
   */
  List<ArticlePurchases> findByPaymentStatusOrderByPurchaseDateDesc(String paymentStatus);

  /**
   * 특정 아티클의 모든 구매 기록 조회 (한국어)
   */
  @Query("SELECT p FROM ArticlePurchases p " +
      "WHERE p.articleIdKo = :articleId " +
      "ORDER BY p.purchaseDate DESC")
  List<ArticlePurchases> findByArticleIdKoOrderByPurchaseDateDesc(@Param("articleId") Long articleId);

  /**
   * 특정 아티클의 모든 구매 기록 조회 (영어)
   */
  @Query("SELECT p FROM ArticlePurchases p " +
      "WHERE p.articleIdEn = :articleId " +
      "ORDER BY p.purchaseDate DESC")
  List<ArticlePurchases> findByArticleIdEnOrderByPurchaseDateDesc(@Param("articleId") Long articleId);

  /**
   * 중복 구매 방지를 위한 체크 (한국어)
   */
  @Query("SELECT p FROM ArticlePurchases p " +
      "WHERE p.userId = :userId AND p.articleIdKo = :articleId " +
      "AND p.paymentStatus IN ('pending', 'completed')")
  List<ArticlePurchases> findActiveKoreanPurchases(
      @Param("userId") Long userId,
      @Param("articleId") Long articleId);

  /**
   * 중복 구매 방지를 위한 체크 (영어)
   */
  @Query("SELECT p FROM ArticlePurchases p " +
      "WHERE p.userId = :userId AND p.articleIdEn = :articleId " +
      "AND p.paymentStatus IN ('pending', 'completed')")
  List<ArticlePurchases> findActiveEnglishPurchases(
      @Param("userId") Long userId,
      @Param("articleId") Long articleId);

  /**
   * 최근 N일간의 매출 통계
   */
  @Query("SELECT COUNT(p), SUM(p.purchasePrice) FROM ArticlePurchases p " +
      "WHERE p.paymentStatus = 'completed' " +
      "AND p.purchaseDate >= :startDate")
  Object[] getRevenueStats(@Param("startDate") Long startDate);

  /**
   * 사용자별 총 구매 금액 조회
   */
  @Query("SELECT SUM(p.purchasePrice) FROM ArticlePurchases p " +
      "WHERE p.userId = :userId AND p.paymentStatus = 'completed'")
  Long getTotalPurchaseAmountByUserId(@Param("userId") Long userId);
}