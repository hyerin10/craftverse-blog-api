package kr.co.craftverse.craftverse_blog_api.repository;

import java.util.List;
import kr.co.craftverse.craftverse_blog_api.model.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
  List<Article> findByLanguage(String language);
}
