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

  @Column(nullable = false)
  private Long amount;

  @Column(name = "order_name", nullable = false)
  private String orderName;

  @Column(name = "customer_email")
  private String customerEmail;

  @Column(name = "customer_name")
  private String customerName;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @Column(name = "created_at", nullable = false)
  private Long createdAt;  // UTC timestamp in milliseconds

  @Column(name = "approved_at")
  private Long approvedAt; // UTC timestamp in milliseconds

  @Column(name = "updated_at")
  private Long updatedAt; // UTC timestamp in milliseconds

  // Payment 생성 시 현재 시간 자동 설정
  @PrePersist
  protected void onCreate() {
    long now = System.currentTimeMillis();
    if (createdAt == null) {
      createdAt = now;
    }
    if (status == null) {
      status = PaymentStatus.READY;
    }
    updatedAt = now;
  }

  // 업데이트 시 시간 자동 설정
  @PreUpdate
  protected void onUpdate() {
    updatedAt = System.currentTimeMillis();
  }

  // 결제 승인 시 승인 시간 설정
  public void approve() {
    this.status = PaymentStatus.DONE;
    this.approvedAt = System.currentTimeMillis();
    this.updatedAt = System.currentTimeMillis();
  }

  // 결제 취소
  public void cancel() {
    this.status = PaymentStatus.CANCELED;
    this.updatedAt = System.currentTimeMillis();
  }

  // 부분 취소
  public void partialCancel() {
    this.status = PaymentStatus.PARTIAL_CANCELED;
    this.updatedAt = System.currentTimeMillis();
  }

  // 결제 실패
  public void abort() {
    this.status = PaymentStatus.ABORTED;
    this.updatedAt = System.currentTimeMillis();
  }

  // 결제 만료
  public void expire() {
    this.status = PaymentStatus.EXPIRED;
    this.updatedAt = System.currentTimeMillis();
  }

  // PaymentKey 설정
  public void setPaymentKey(String paymentKey) {
    this.paymentKey = paymentKey;
    this.updatedAt = System.currentTimeMillis();
  }

  // 상태 업데이트
  public void updateStatus(PaymentStatus status) {
    this.status = status;
    this.updatedAt = System.currentTimeMillis();
    if (status == PaymentStatus.DONE && this.approvedAt == null) {
      this.approvedAt = System.currentTimeMillis();
    }
  }

  // 결제 가능한 상태인지 확인
  public boolean isPayable() {
    return this.status == PaymentStatus.READY;
  }

  // 취소 가능한 상태인지 확인
  public boolean isCancelable() {
    return this.status == PaymentStatus.DONE || this.status == PaymentStatus.PARTIAL_CANCELED;
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
    long now = System.currentTimeMillis();
    return Payment.builder()
        .orderId(orderId)
        .amount(amount)
        .orderName(orderName)
        .customerEmail(customerEmail)
        .customerName(customerName)
        .userId(userId)
        .status(PaymentStatus.READY)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}