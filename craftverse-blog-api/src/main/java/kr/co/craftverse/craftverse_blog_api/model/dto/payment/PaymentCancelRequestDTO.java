package kr.co.craftverse.craftverse_blog_api.model.dto.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCancelRequestDTO {

  @NotBlank(message = "취소 사유는 필수입니다")
  private String cancelReason;

  private Long cancelAmount; // null이면 전액 취소
}