package kr.co.craftverse.craftverse_blog_api.exception;

public class InvalidPaymentStatusException extends RuntimeException {
  public InvalidPaymentStatusException(String message) { super(message); }
}
