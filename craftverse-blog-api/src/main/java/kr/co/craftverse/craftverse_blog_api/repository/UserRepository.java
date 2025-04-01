package kr.co.craftverse.craftverse_blog_api.repository;

import java.util.Optional;
import kr.co.craftverse.craftverse_blog_api.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);
  boolean existsByEmail(String email);
}
