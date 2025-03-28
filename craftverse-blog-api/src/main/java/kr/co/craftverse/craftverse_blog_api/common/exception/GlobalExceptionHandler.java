package kr.co.craftverse.craftverse_blog_api.common.exception;

import kr.co.craftverse.craftverse_blog_api.common.RestError;
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
      EmptyDataException.class
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public RestError handleBadRequest(Exception  e) {
    log.error("Bad request", e);
    return new RestError(HttpStatus.BAD_REQUEST, e.getMessage());
  }
}
