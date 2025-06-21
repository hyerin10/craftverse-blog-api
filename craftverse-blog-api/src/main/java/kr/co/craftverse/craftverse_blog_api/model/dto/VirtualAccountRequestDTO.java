package kr.co.craftverse.craftverse_blog_api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VirtualAccountRequestDTO {

  @NotNull(message = "결제 금액은 필수입니다")
  @Positive(message = "결제 금액은 0보다 커야 합니다")
  private Long amount;

  @NotBlank(message = "상품명은 필수입니다")
  private String orderName;

  @NotBlank(message = "구매자 이름은 필수입니다")
  private String customerName;

  @NotBlank(message = "은행 코드는 필수입니다")
  private String bank;

  private String customerEmail;
  private String customerMobilePhone;
  private Integer validHours; // 가상계좌 유효 시간
}