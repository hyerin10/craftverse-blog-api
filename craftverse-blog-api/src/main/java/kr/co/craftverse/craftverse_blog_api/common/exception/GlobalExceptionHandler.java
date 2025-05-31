package kr.co.craftverse.craftverse_blog_api.common.exception;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.validation.ConstraintViolationException;
import kr.co.craftverse.craftverse_blog_api.common.RestError;
import kr.co.craftverse.craftverse_blog_api.exception.DuplicateResourceException;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.NotFoundException;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
  @ExceptionHandler({
      DuplicateResourceException.class,
      MethodArgumentNotValidException.class,
      ConstraintViolationException.class,
      MissingServletRequestParameterException.class
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public RestError handleBadRequest(Exception  e) {
    String errorMessage = "Bad Request";
    log.error(errorMessage, e);
    return new RestError(HttpStatus.BAD_REQUEST, errorMessage);
  }

  @ExceptionHandler({
      HttpRequestMethodNotSupportedException.class
  })
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public RestError handleMethodNotAllowed(Exception  e) {
    String errorMessage = "Method Not Allowed";
    log.error(errorMessage, e);
    return new RestError(HttpStatus.METHOD_NOT_ALLOWED, errorMessage);
  }

  @ExceptionHandler({
      NotFoundException.class
  })
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public RestError handleNotFound(Exception  e) {
    log.error("Not Found", e);
    return new RestError(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler({UnauthorizedException.class,
      MalformedJwtException.class})
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public RestError handleUnauthorizedException(Exception e) {
    String errorMessage = "";
    log.error(errorMessage, e);
    return new RestError(HttpStatus.UNAUTHORIZED, errorMessage);
  }
}
