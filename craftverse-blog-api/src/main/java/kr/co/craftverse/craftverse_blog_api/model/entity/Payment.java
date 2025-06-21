package kr.co.craftverse.craftverse_blog_api.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id", unique = true, nullable = false)
  private String orderId;

  @Column(name = "payment_key")
  private String paymentKey;

  private Long amount;

  @Column(name = "order_name")
  private String orderName;

  @Column(name = "customer_email")
  private String customerEmail;

  @Column(name = "customer_name")
  private String customerName;

  @Column(name = "user_id")
  private Long userId;

  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  @Column(name = "created_at")
  private Long createdAt;  // UTC timestamp in milliseconds

  @Column(name = "approved_at")
  private Long approvedAt; // UTC timestamp in milliseconds

  // Payment 생성 시 현재 시간 자동 설정
  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = System.currentTimeMillis();
    }
    if (status == null) {
      status = PaymentStatus.READY;
    }
  }

  // 결제 승인 시 승인 시간 설정
  public void approve() {
    this.status = PaymentStatus.DONE;
    this.approvedAt = System.currentTimeMillis();
  }

  // 결제 취소
  public void cancel() {
    this.status = PaymentStatus.CANCELED;
  }

  // 부분 취소
  public void partialCancel() {
    this.status = PaymentStatus.PARTIAL_CANCELED;
  }

  // 결제 실패
  public void abort() {
    this.status = PaymentStatus.ABORTED;
  }

  // 결제 만료
  public void expire() {
    this.status = PaymentStatus.EXPIRED;
  }

  // PaymentKey 설정
  public void setPaymentKey(String paymentKey) {
    this.paymentKey = paymentKey;
  }

  // 상태 업데이트
  public void updateStatus(PaymentStatus status) {
    this.status = status;
    if (status == PaymentStatus.DONE && this.approvedAt == null) {
      this.approvedAt = System.currentTimeMillis();
    }
  }

  public enum PaymentStatus {
    READY,              // 결제 준비
    IN_PROGRESS,        // 결제 진행중
    WAITING_FOR_DEPOSIT,// 입금 대기 (가상계좌)
    DONE,              // 결제 완료
    CANCELED,          // 결제 취소
    PARTIAL_CANCELED,  // 부분 취소
    ABORTED,           // 결제 실패
    EXPIRED            // 결제 만료
  }

  // 빌더 패턴을 위한 정적 팩토리 메서드
  public static Payment createPayment(String orderId, Long amount, String orderName,
      String customerEmail, String customerName, Long userId) {
    return Payment.builder()
        .orderId(orderId)
        .amount(amount)
        .orderName(orderName)
        .customerEmail(customerEmail)
        .customerName(customerName)
        .userId(userId)
        .status(PaymentStatus.READY)
        .createdAt(System.currentTimeMillis())
        .build();
  }
}