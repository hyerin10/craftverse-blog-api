package kr.co.craftverse.craftverse_blog_api.common.exception;

import io.jsonwebtoken.MalformedJwtException;
import kr.co.craftverse.craftverse_blog_api.common.RestError;
import kr.co.craftverse.craftverse_blog_api.exception.AuthenticationFailureException;
import kr.co.craftverse.craftverse_blog_api.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
  @ExceptionHandler({
      EmptyDataException.class,
      DuplicateResourceException.class
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public RestError handleBadRequest(Exception  e) {
    log.error("Bad request", e);
    return new RestError(HttpStatus.BAD_REQUEST, e.getMessage());
  }
  @ExceptionHandler({AuthenticationFailureException.class,
      MalformedJwtException.class})
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public RestError handleAuthenticationFailureException(Exception e) {
    log.error("Fail to Authentication.", e);
    String errorMessage = "Fail to Authentication.";
    return new RestError(HttpStatus.UNAUTHORIZED, errorMessage);
  }
}
