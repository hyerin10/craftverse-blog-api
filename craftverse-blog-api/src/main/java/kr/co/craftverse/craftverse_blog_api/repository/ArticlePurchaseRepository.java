package kr.co.craftverse.craftverse_blog_api.repository;

import java.util.List;
import kr.co.craftverse.craftverse_blog_api.model.entity.ArticlePurchases;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticlePurchaseRepository extends JpaRepository<ArticlePurchases, Long> {
  List<ArticlePurchases> findByUserId(Long userId);
}
