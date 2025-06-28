package kr.co.craftverse.craftverse_blog_api.model.dto.auth;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PASSWORD_REGEX;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.PASSWORD_VALIDATION_MESSAGE;

import jakarta.validation.constraints.Email;
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
public class UserUpdateRequestDTO {

  @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
  private String firstName;

  @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
  private String lastName;

  @Email(message = "Email should be valid")
  private String email;

  @Size(min = 8, message = "Password must be at least 8 characters")
  @Pattern(regexp = PASSWORD_REGEX,
      message = PASSWORD_VALIDATION_MESSAGE)
  private String password;

  private Long birthDate;

  private String country;

  private String postalCode;
}