package kr.co.craftverse.craftverse_blog_api.exception.payment;

public class PaymentProcessException extends RuntimeException {
  public PaymentProcessException(String message) { super(message); }
}