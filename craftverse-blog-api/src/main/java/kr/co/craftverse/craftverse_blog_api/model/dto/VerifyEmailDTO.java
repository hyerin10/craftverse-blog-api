package kr.co.craftverse.craftverse_blog_api.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailDTO {

  @NotBlank(message = "이메일은 필수입니다.")
  @Email(message = "유효한 이메일 형식이 아닙니다.")
  private String email;

  @NotBlank(message = "인증 코드는 필수입니다.")
  private String code;
}
