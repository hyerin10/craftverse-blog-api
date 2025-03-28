package kr.co.craftverse.craftverse_blog_api.common.exception;

public class EmptyDataException extends RuntimeException {
  public EmptyDataException(String message) {
    super(message);
  }
}
