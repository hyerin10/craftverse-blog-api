package kr.co.craftverse.craftverse_blog_api.exception;

public class PaymentAmountMismatchException extends RuntimeException {
  public PaymentAmountMismatchException(String message) { super(message); }
}