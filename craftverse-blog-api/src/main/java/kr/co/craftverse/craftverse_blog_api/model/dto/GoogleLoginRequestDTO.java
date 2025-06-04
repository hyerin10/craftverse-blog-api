package kr.co.craftverse.craftverse_blog_api.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoogleLoginRequestDTO {
  private String code;
  private String state;
  private String redirectUri;
  private String error;
}
