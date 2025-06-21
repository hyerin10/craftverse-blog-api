package kr.co.craftverse.craftverse_blog_api.service;

import kr.co.craftverse.craftverse_blog_api.model.entity.ArticlePurchases;
import kr.co.craftverse.craftverse_blog_api.repository.ArticlePurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {
  private final ArticlePurchaseRepository articlePurchaseRepository;
  private final ArticleService articleService;

  @Transactional
  public void completePurchase(ArticlePurchases purchase) {
    // 구매 정보 저장
    ArticlePurchases savedPurchase = articlePurchaseRepository.save(purchase);

    // 해당 사용자의 모든 구매 관련 캐시 무효화
    articleService.clearAllPurchaseCacheForUser(savedPurchase.getUserId());

    log.info("Purchase completed and all cache cleared for user: {}", savedPurchase.getUserId());
  }
}