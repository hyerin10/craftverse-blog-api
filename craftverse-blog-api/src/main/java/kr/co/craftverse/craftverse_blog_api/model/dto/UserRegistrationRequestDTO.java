package kr.co.craftverse.craftverse_blog_api.model.dto;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PASSWORD_REGEX;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PASSWORD_VALIDATION_MESSAGE;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequestDTO {
  @NotBlank(message = "First name is required")
  @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
  private String firstName;

  @NotBlank(message = "Last name is required")
  @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
  private String lastName;

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  @Pattern(regexp = PASSWORD_REGEX,
      message = PASSWORD_VALIDATION_MESSAGE)
  private String password;

  private Long birthDate;

  private String country;

  private String postalCode;
}
