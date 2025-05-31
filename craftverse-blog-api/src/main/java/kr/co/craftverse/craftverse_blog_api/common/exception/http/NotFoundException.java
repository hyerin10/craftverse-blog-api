package kr.co.craftverse.craftverse_blog_api.common.exception.http;

public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) {
    super(message);
  }
}