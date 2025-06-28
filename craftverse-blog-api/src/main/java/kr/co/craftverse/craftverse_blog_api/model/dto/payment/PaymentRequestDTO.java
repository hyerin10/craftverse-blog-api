package kr.co.craftverse.craftverse_blog_api.model.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequestDTO {

  @NotNull(message = "결제 금액은 필수입니다")
  @Positive(message = "결제 금액은 0보다 커야 합니다")
  private Long amount;

  @NotBlank(message = "상품명은 필수입니다")
  private String orderName;

  @NotBlank(message = "구매자 이름은 필수입니다")
  private String customerName;

  private String customerEmail;
  private String customerMobilePhone;

  // 아티클 구매를 위한 필드 추가
  @NotNull(message = "아티클 ID는 필수입니다")
  @Positive(message = "아티클 ID는 0보다 커야 합니다")
  private Long articleId;

  @NotBlank(message = "언어 설정은 필수입니다")
  private String language;
}