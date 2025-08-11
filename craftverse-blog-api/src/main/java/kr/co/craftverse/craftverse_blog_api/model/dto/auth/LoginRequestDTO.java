package kr.co.craftverse.craftverse_blog_api.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {
  @NotBlank(message = "Email is a required entry.")
  private String email;

  @NotBlank(message = "Password is a required entry.")
  private String password;
}
