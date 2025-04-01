package kr.co.craftverse.craftverse_blog_api.exception;

public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException() {
    super("Fail to authorize.");
  }
}