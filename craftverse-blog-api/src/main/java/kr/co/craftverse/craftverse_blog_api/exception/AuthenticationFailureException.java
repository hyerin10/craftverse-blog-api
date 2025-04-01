package kr.co.craftverse.craftverse_blog_api.exception;

public class AuthenticationFailureException extends RuntimeException {
  public AuthenticationFailureException() {
    super("Fail to Authentication.");
  }
}
