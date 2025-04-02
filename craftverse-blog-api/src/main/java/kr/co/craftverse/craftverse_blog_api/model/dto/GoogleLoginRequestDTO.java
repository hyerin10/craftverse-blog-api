package kr.co.craftverse.craftverse_blog_api.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class GoogleLoginRequestDTO {
  @NotBlank(message = "인증 코드는 필수입니다.")
  private String code;

  private String redirectUri;
}
