package kr.co.craftverse.craftverse_blog_api.exception;

public class PaymentNotFoundException extends RuntimeException {
  public PaymentNotFoundException(String message) { super(message); }
}
