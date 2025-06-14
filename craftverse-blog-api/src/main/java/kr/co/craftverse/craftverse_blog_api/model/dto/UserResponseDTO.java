package kr.co.craftverse.craftverse_blog_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
  private Long id;
  private String firstName;
  private String lastName;
  private String email;
  private Long birthDate;
  private String country;
  private String postalCode;
  private Long createdAt;
  private Long updatedAt;
  private Boolean emailVerified;
}
