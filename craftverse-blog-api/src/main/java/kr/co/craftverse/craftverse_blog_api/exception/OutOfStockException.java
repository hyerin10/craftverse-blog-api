package kr.co.craftverse.craftverse_blog_api.exception;

public class OutOfStockException extends RuntimeException {
  public OutOfStockException(String message) { super(message); }
}
