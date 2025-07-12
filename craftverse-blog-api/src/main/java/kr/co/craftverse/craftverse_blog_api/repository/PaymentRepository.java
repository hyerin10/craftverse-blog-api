package kr.co.craftverse.craftverse_blog_api.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import kr.co.craftverse.craftverse_blog_api.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findById(Long id);
  Optional<Payment> findByOrderId(String orderId);
  Optional<Payment> findByPaymentKey(String paymentKey);
  Optional<Payment> findByOrderIdAndUserId(String orderId, Long userId);

  List<Payment> findByUserId(Long userId);
  List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
  List<Payment> findByStatus(Payment.PaymentStatus status);
  List<Payment> findByUserIdAndStatus(Long userId, Payment.PaymentStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.userId = :userId")
  Optional<Payment> findByOrderIdAndUserIdForUpdate(@Param("orderId") String orderId, @Param("userId") Long userId);
}