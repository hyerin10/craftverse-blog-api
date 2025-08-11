package kr.co.craftverse.craftverse_blog_api.model.dto.payment;

import kr.co.craftverse.craftverse_blog_api.model.entity.Payment;
import kr.co.craftverse.craftverse_blog_api.common.util.TimeUtils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResponseDTO {

  private Long id;
  private String orderId;
  private String paymentKey;
  private Long amount;
  private String orderName;
  private String customerEmail;
  private String customerName;
  private Long userId;
  private Payment.PaymentStatus status;
  private Long createdAt;        // UTC milliseconds
  private Long approvedAt;       // UTC milliseconds
  private String createdAtIso;   // ISO 8601 format
  private String approvedAtIso;  // ISO 8601 format

  public static PaymentResponseDTO from(Payment payment) {
    PaymentResponseDTO dto = new PaymentResponseDTO();
    dto.setId(payment.getId());
    dto.setOrderId(payment.getOrderId());
    dto.setPaymentKey(payment.getPaymentKey());
    dto.setAmount(payment.getAmount());
    dto.setOrderName(payment.getOrderName());
    dto.setCustomerEmail(payment.getCustomerEmail());
    dto.setCustomerName(payment.getCustomerName());
    dto.setUserId(payment.getUserId());
    dto.setStatus(payment.getStatus());
    dto.setCreatedAt(payment.getCreatedAt());
    dto.setApprovedAt(payment.getApprovedAt());

    // ISO 8601 형식으로도 제공
    dto.setCreatedAtIso(TimeUtils.toIsoString(payment.getCreatedAt()));
    dto.setApprovedAtIso(TimeUtils.toIsoString(payment.getApprovedAt()));

    return dto;
  }
}