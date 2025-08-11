package kr.co.craftverse.craftverse_blog_api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestError {
  private HttpStatus code;
  private String message;
  public RestError(HttpStatus code, String message) {
    this.code = code;
    this.message = message;
  }
}