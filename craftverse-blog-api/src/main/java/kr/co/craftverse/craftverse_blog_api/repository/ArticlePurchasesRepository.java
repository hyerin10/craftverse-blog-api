package kr.co.craftverse.craftverse_blog_api.repository;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.*;

import kr.co.craftverse.craftverse_blog_api.model.entity.ArticlePurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticlePurchasesRepository extends JpaRepository<ArticlePurchase, Long> {

  /**
   * 사용자의 모든 구매 기록 조회
   */
  List<ArticlePurchase> findByUserId(Long userId);

  /**
   * 사용자의 구매 기록을 구매일 순으로 조회 (최신순)
   */
  List<ArticlePurchase> findByUserIdOrderByPurchaseDateDesc(Long userId);

  /**
   * 결제 키로 구매 기록 조회
   */
  Optional<ArticlePurchase> findByPaymentKey(String paymentKey);

  /**
   * 주문 ID로 구매 기록 조회
   */
  Optional<ArticlePurchase> findByOrderId(String orderId);

  /**
   * 사용자의 특정 결제 상태 구매 기록 조회
   */
  List<ArticlePurchase> findByUserIdAndPaymentStatus(Long userId, String paymentStatus);

  /**
   * 사용자의 완료된 구매 기록만 조회 (최신순)
   */
  List<ArticlePurchase> findByUserIdAndPaymentStatusOrderByPurchaseDateDesc(
      Long userId, String paymentStatus);

  /**
   * 한국어 아티클 구매 여부 확인 (완료된 결제만)
   */
  @Query("SELECT COUNT(p) > 0 FROM ArticlePurchase p " +
      "WHERE p.userId = :userId AND p.articleIdKo = :articleId " +
      "AND p.paymentStatus = '" + PAYMENT_STATUS_COMPLETED + "'")
  boolean existsByUserIdAndArticleIdKoAndCompleted(
      @Param("userId") Long userId,
      @Param("articleId") Long articleId);

  /**
   * 영어 아티클 구매 여부 확인 (완료된 결제만)
   */
  @Query("SELECT COUNT(p) > 0 FROM ArticlePurchase p " +
      "WHERE p.userId = :userId AND p.articleIdEn = :articleId " +
      "AND p.paymentStatus = '" + PAYMENT_STATUS_COMPLETED + "'")
  boolean existsByUserIdAndArticleIdEnAndCompleted(
      @Param("userId") Long userId,
      @Param("articleId") Long articleId);

  /**
   * 사용자의 특정 아티클 구매 기록 조회 (언어별, 완료된 결제만)
   */
  @Query("SELECT p FROM ArticlePurchase p " +
      "WHERE p.userId = :userId " +
      "AND ((:language = '" + LANGUAGE_KO + "' AND p.articleIdKo = :articleId) " +
      "     OR (:language = '" + LANGUAGE_EN + "' AND p.articleIdEn = :articleId)) " +
      "AND p.paymentStatus = '" + PAYMENT_STATUS_COMPLETED + "'")
  Optional<ArticlePurchase> findByUserIdAndArticleIdAndLanguageAndCompleted(
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
  @Query("SELECT COUNT(DISTINCT p.userId) FROM ArticlePurchase p " +
      "WHERE p.articleIdKo = :articleId AND p.paymentStatus = '" + PAYMENT_STATUS_COMPLETED + "'")
  long countDistinctUsersByArticleIdKoAndCompleted(@Param("articleId") Long articleId);

  /**
   * 특정 아티클의 구매자 수 조회 (영어, 완료된 결제만)
   */
  @Query("SELECT COUNT(DISTINCT p.userId) FROM ArticlePurchase p " +
      "WHERE p.articleIdEn = :articleId AND p.paymentStatus = '" + PAYMENT_STATUS_COMPLETED + "'")
  long countDistinctUsersByArticleIdEnAndCompleted(@Param("articleId") Long articleId);

  /**
   * 특정 기간 내 구매 기록 조회
   */
  @Query("SELECT p FROM ArticlePurchase p " +
      "WHERE p.purchaseDate >= :startDate AND p.purchaseDate <= :endDate " +
      "ORDER BY p.purchaseDate DESC")
  List<ArticlePurchase> findByPurchaseDateBetween(
      @Param("startDate") Long startDate,
      @Param("endDate") Long endDate);

  /**
   * 사용자의 특정 기간 내 구매 기록 조회
   */
  @Query("SELECT p FROM ArticlePurchase p " +
      "WHERE p.userId = :userId " +
      "AND p.purchaseDate >= :startDate AND p.purchaseDate <= :endDate " +
      "ORDER BY p.purchaseDate DESC")
  List<ArticlePurchase> findByUserIdAndPurchaseDateBetween(
      @Param("userId") Long userId,
      @Param("startDate") Long startDate,
      @Param("endDate") Long endDate);

  /**
   * 결제 상태별 구매 기록 조회
   */
  List<ArticlePurchase> findByPaymentStatusOrderByPurchaseDateDesc(String paymentStatus);

  /**
   * 특정 아티클의 모든 구매 기록 조회 (한국어)
   */
  @Query("SELECT p FROM ArticlePurchase p " +
      "WHERE p.articleIdKo = :articleId " +
      "ORDER BY p.purchaseDate DESC")
  List<ArticlePurchase> findByArticleIdKoOrderByPurchaseDateDesc(@Param("articleId") Long articleId);

  /**
   * 특정 아티클의 모든 구매 기록 조회 (영어)
   */
  @Query("SELECT p FROM ArticlePurchase p " +
      "WHERE p.articleIdEn = :articleId " +
      "ORDER BY p.purchaseDate DESC")
  List<ArticlePurchase> findByArticleIdEnOrderByPurchaseDateDesc(@Param("articleId") Long articleId);

  /**
   * 중복 구매 방지를 위한 체크 (한국어)
   */
  @Query("SELECT p FROM ArticlePurchase p " +
      "WHERE p.userId = :userId AND p.articleIdKo = :articleId " +
      "AND p.paymentStatus IN ('" + PAYMENT_STATUS_PENDING + "', '" + PAYMENT_STATUS_COMPLETED + "')")
  List<ArticlePurchase> findActiveKoreanPurchases(
      @Param("userId") Long userId,
      @Param("articleId") Long articleId);

  /**
   * 중복 구매 방지를 위한 체크 (영어)
   */
  @Query("SELECT p FROM ArticlePurchase p " +
      "WHERE p.userId = :userId AND p.articleIdEn = :articleId " +
      "AND p.paymentStatus IN ('" + PAYMENT_STATUS_PENDING + "', '" + PAYMENT_STATUS_COMPLETED + "')")
  List<ArticlePurchase> findActiveEnglishPurchases(
      @Param("userId") Long userId,
      @Param("articleId") Long articleId);

  /**
   * 최근 N일간의 매출 통계
   */
  @Query("SELECT COUNT(p), SUM(p.purchasePrice) FROM ArticlePurchase p " +
      "WHERE p.paymentStatus = '" + PAYMENT_STATUS_COMPLETED + "' " +
      "AND p.purchaseDate >= :startDate")
  Object[] getRevenueStats(@Param("startDate") Long startDate);

  /**
   * 사용자별 총 구매 금액 조회
   */
  @Query("SELECT SUM(p.purchasePrice) FROM ArticlePurchase p " +
      "WHERE p.userId = :userId AND p.paymentStatus = '" + PAYMENT_STATUS_COMPLETED + "'")
  Long getTotalPurchaseAmountByUserId(@Param("userId") Long userId);
}