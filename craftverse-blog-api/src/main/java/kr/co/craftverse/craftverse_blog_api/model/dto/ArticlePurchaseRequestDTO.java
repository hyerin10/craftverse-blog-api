package kr.co.craftverse.craftverse_blog_api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 아티클 구매 완료 처리 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticlePurchaseRequestDTO {

  @NotNull(message = "아티클 ID는 필수입니다")
  @Positive(message = "아티클 ID는 0보다 커야 합니다")
  private Long articleId;

  @NotBlank(message = "결제 키는 필수입니다")
  private String paymentKey;

  @NotBlank(message = "주문 ID는 필수입니다")
  private String orderId;

  @NotNull(message = "결제 금액은 필수입니다")
  @Positive(message = "결제 금액은 0보다 커야 합니다")
  private Long amount;

  @NotBlank(message = "언어 설정은 필수입니다")
  private String language;
}