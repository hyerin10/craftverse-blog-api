package kr.co.craftverse.craftverse_blog_api.repository;

import java.util.List;
import java.util.Optional;
import kr.co.craftverse.craftverse_blog_api.model.dto.ArticleDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
  Optional<Article> findById(long id);
  List<Article> findByLanguage(String language);
}
