package kr.co.craftverse.craftverse_blog_api.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RestResult<T> {
  private T result;
}