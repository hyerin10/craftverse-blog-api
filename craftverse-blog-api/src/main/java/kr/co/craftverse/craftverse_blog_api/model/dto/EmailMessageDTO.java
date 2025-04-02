package kr.co.craftverse.craftverse_blog_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessageDTO {
  private String to;
  private String subject;
  private String content;
  private String verificationCode;
}
