package kr.co.craftverse.craftverse_blog_api.common.exception.http;

public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException() {
    super("Fail to authorize.");
  }
}