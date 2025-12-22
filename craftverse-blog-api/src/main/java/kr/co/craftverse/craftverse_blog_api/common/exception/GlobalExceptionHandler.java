package kr.co.craftverse.craftverse_blog_api.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.validation.ConstraintViolationException;
import java.net.SocketTimeoutException;
import kr.co.craftverse.craftverse_blog_api.common.RestError;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.NotFoundException;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.exception.DuplicateResourceException;
import kr.co.craftverse.craftverse_blog_api.exception.InvalidPaymentStatusException;
import kr.co.craftverse.craftverse_blog_api.exception.payment.PaymentAmountMismatchException;
import kr.co.craftverse.craftverse_blog_api.exception.payment.PaymentNotFoundException;
import kr.co.craftverse.craftverse_blog_api.exception.payment.PaymentProcessException;
import kr.co.craftverse.craftverse_blog_api.exception.payment.TossPaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.TypeMismatchException;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  /**
   * 공통 로깅 처리 메서드
   * @param e 발생한 예외 객체 (Stack Trace 보존용)
   * @param httpStatus 반환할 HTTP 상태
   * @param logMessage 로그에 남길 요약 메시지
   */
  private void logWithMdc(Exception e, HttpStatus httpStatus, String logMessage) {
    MDC.put("httpStatus", String.valueOf(httpStatus.value()));
    MDC.put("exceptionClass", e.getClass().getSimpleName());
    MDC.put("errorMessage", e.getMessage());

    if (httpStatus.is5xxServerError()) {
      // 500번대 에러는 서비스 장애 상황이므로 전체 Stack Trace를 로깅
      log.error("{} - Exception: {}, Message: {}", logMessage, e.getClass().getSimpleName(), e.getMessage(), e);
    } else {
      // 400번대 에러는 클라이언트 실수일 확률이 높으므로 한 줄만 기록 (Stack Trace 제외 가능)
      // 필요하다면 여기서도 e를 인자로 넘겨서 상세히 볼 수 있습니다.
      log.warn("{} - Exception: {}, Message: {}", logMessage, e.getClass().getSimpleName(), e.getMessage());
    }
  }

  // =================================
  // 400 BAD REQUEST
  // =================================
  @ExceptionHandler({
      DuplicateResourceException.class,
      MethodArgumentNotValidException.class,
      ConstraintViolationException.class,
      MissingServletRequestParameterException.class,
      ArithmeticException.class,
      IllegalArgumentException.class,
      HttpMessageNotReadableException.class,
      HttpMediaTypeNotSupportedException.class,
      MultipartException.class,
      TypeMismatchException.class,
      MethodArgumentTypeMismatchException.class,
      JsonProcessingException.class,
      InvalidPaymentStatusException.class,
      PaymentAmountMismatchException.class
  })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public RestError handleBadRequest(Exception e) {
    logWithMdc(e, HttpStatus.BAD_REQUEST, "Bad Request");
    return new RestError(HttpStatus.BAD_REQUEST, "Bad Request");
  }

  // =================================
  // 401 UNAUTHORIZED
  // =================================
  @ExceptionHandler({
      UnauthorizedException.class,
      AuthenticationException.class,
      BadCredentialsException.class,
      OAuth2AuthenticationException.class,
      MalformedJwtException.class,
      ExpiredJwtException.class,
      JwtException.class
  })
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public RestError handleUnauthorized(Exception e) {
    logWithMdc(e, HttpStatus.UNAUTHORIZED, "Unauthorized");
    return new RestError(HttpStatus.UNAUTHORIZED, "Unauthorized");
  }

  // =================================
  // 403 FORBIDDEN
  // =================================
  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public RestError handleForbidden(AccessDeniedException e) {
    logWithMdc(e, HttpStatus.FORBIDDEN, "Forbidden");
    return new RestError(HttpStatus.FORBIDDEN, "Forbidden");
  }

  // =================================
  // 404 NOT FOUND
  // =================================
  @ExceptionHandler({
      NotFoundException.class,
      NoHandlerFoundException.class,
      PaymentNotFoundException.class
  })
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public RestError handleNotFound(Exception e) {
    logWithMdc(e, HttpStatus.NOT_FOUND, "Not Found");
    return new RestError(HttpStatus.NOT_FOUND, "Not Found");
  }

  // =================================
  // 405 METHOD NOT ALLOWED
  // =================================
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public RestError handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
    logWithMdc(e, HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");
    return new RestError(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");
  }

  // =================================
  // 409 CONFLICT
  // =================================
  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public RestError handleConflict(DataIntegrityViolationException e) {
    logWithMdc(e, HttpStatus.CONFLICT, "Data Integrity Violation");
    return new RestError(HttpStatus.CONFLICT, "Conflict");
  }

  // =================================
  // 500 INTERNAL SERVER ERROR
  // =================================
  @ExceptionHandler({
      RuntimeException.class,
      Exception.class,
      DataAccessException.class,
      PaymentProcessException.class
  })
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public RestError handleInternalServerError(Exception e) {
    // 500 에러는 원인 파악이 중요하므로 logWithMdc 내부에서 전체 스택 트레이스(e)를 출력함
    logWithMdc(e, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
    return new RestError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
  }

  // =================================
  // 502 BAD GATEWAY
  // =================================
  @ExceptionHandler({
      HttpClientErrorException.class,
      HttpServerErrorException.class,
      ResourceAccessException.class,
      SocketTimeoutException.class,
      TossPaymentException.class
  })
  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  public RestError handleBadGateway(Exception e) {
    logWithMdc(e, HttpStatus.BAD_GATEWAY, "Bad Gateway (External API)");
    return new RestError(HttpStatus.BAD_GATEWAY, "Bad Gateway");
  }
}